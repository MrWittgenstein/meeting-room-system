using System;
using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using UnityEngine;

namespace NordicRoom
{
    /// <summary>
    /// 树莓派 WebSocket 客户端。
    ///
    /// 设计要点：
    ///  · 后台线程负责连接与收包（ClientWebSocket 是异步 API，不能阻塞主线程）；
    ///  · 收到的原始 JSON 先丢进线程安全队列，**只在主线程的 Update 里解析并派发**，
    ///    这样回调里可以安全地操作 Unity 对象（Transform / Material 都是主线程限定）；
    ///  · 断线后每 reconnectDelay 秒自动重连；
    ///  · 不依赖任何第三方插件（Unity 自带 .NET 的 ClientWebSocket）。
    ///
    /// 使用方式：由 RoomController 在运行时自动创建，无需手动挂到场景里。
    /// </summary>
    [DisallowMultipleComponent]
    public class PiWebSocketClient : MonoBehaviour
    {
        [Header("连接")]
        public string url = "ws://192.168.43.232:8775/";
        public float reconnectDelay = 2f;
        public bool autoConnect = true;

        [Header("调试")]
        public bool logRawMessages = false;

        /// <summary>是否已连上。</summary>
        public bool IsConnected { get; private set; }

        /// <summary>已解析并派发的帧数。</summary>
        public int FramesReceived { get; private set; }

        /// <summary>最近一次成功收到数据的主线程时间（unscaledTime）。</summary>
        public float LastMessageTime { get; private set; } = -1f;

        /// <summary>最近一次错误信息（连接失败 / 解析失败），供 HUD 显示。</summary>
        public string LastError { get; private set; } = "";

        /// <summary>最近一条原始报文（排查格式问题用）。</summary>
        public string LastRawSample { get; private set; } = "";

        /// <summary>每收到一帧真实数据触发（主线程）。</summary>
        public event Action<PiFrame> FrameReceived;

        /// <summary>连接状态变化时触发（主线程）。</summary>
        public event Action<bool> ConnectionChanged;

        readonly ConcurrentQueue<string> _incoming = new ConcurrentQueue<string>();

        ClientWebSocket _socket;
        CancellationTokenSource _cts;
        Task _loop;
        volatile bool _connected;

        bool _warnedPlatform;

        // ================================================================= 生命周期

        void Start()
        {
            if (autoConnect) Connect();
        }

        void OnDisable() => Disconnect();
        void OnDestroy() => Disconnect();

        // ================================================================= 连接控制

        public void Connect()
        {
            if (Application.platform == RuntimePlatform.WebGLPlayer)
            {
                if (!_warnedPlatform)
                {
                    _warnedPlatform = true;
                    LastError = "WebGL 下不支持原生 WebSocket，需要改用 jslib 或后端代理";
                    Debug.LogWarning("[PiWS] " + LastError);
                }
                return;
            }

            Disconnect();

            _cts = new CancellationTokenSource();
            var token = _cts.Token;
            _loop = RunLoopAsync(token);
        }

        public void Disconnect()
        {
            try { _cts?.Cancel(); } catch { }
            try { _socket?.Abort(); } catch { }
            try { _socket?.Dispose(); } catch { }
            _socket = null;
            try { _cts?.Dispose(); } catch { }
            _cts = null;

            if (_connected) SetConnected(false);
        }

        /// <summary>手动重连（HUD 的「重连」按钮调用）。</summary>
        public void Reconnect()
        {
            LastError = "";
            Connect();
        }

        // ================================================================= 后台收发

        async Task RunLoopAsync(CancellationToken token)
        {
            while (!token.IsCancellationRequested)
            {
                ClientWebSocket ws = null;
                try
                {
                    ws = new ClientWebSocket();
                    _socket = ws;

                    await ws.ConnectAsync(new Uri(url), token).ConfigureAwait(false);
                    SetConnected(true);

                    var buffer = new byte[8192];
                    var sb = new StringBuilder();

                    while (ws.State == WebSocketState.Open && !token.IsCancellationRequested)
                    {
                        var result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), token).ConfigureAwait(false);

                        if (result.MessageType == WebSocketMessageType.Close)
                        {
                            try
                            {
                                await ws.CloseOutputAsync(WebSocketCloseStatus.NormalClosure, "", CancellationToken.None)
                                        .ConfigureAwait(false);
                            }
                            catch { }
                            break;
                        }

                        if (result.Count > 0)
                            sb.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));

                        // 一条完整消息可能分多个分片，EndOfMessage 才算收完
                        if (result.EndOfMessage)
                        {
                            var text = sb.ToString();
                            sb.Clear();
                            if (!string.IsNullOrWhiteSpace(text)) _incoming.Enqueue(text);
                        }
                    }
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (Exception e)
                {
                    LastError = e.GetType().Name + ": " + e.Message;
                }
                finally
                {
                    SetConnected(false);
                    try { ws?.Dispose(); } catch { }
                    if (ReferenceEquals(_socket, ws)) _socket = null;
                }

                if (token.IsCancellationRequested) break;

                try { await Task.Delay(TimeSpan.FromSeconds(Mathf.Max(0.5f, reconnectDelay)), token).ConfigureAwait(false); }
                catch (OperationCanceledException) { break; }
            }
        }

        void SetConnected(bool value)
        {
            // 后台线程只改标记，事件统一在主线程的 Update 里补发
            _connected = value;
        }

        bool _eventState;

        // ================================================================= 主线程

        void Update()
        {
            // 连接状态变化 → 主线程派发事件
            if (_eventState != _connected)
            {
                _eventState = _connected;
                IsConnected = _connected;
                try { ConnectionChanged?.Invoke(IsConnected); } catch (Exception e) { Debug.LogException(e); }
            }

            while (_incoming.TryDequeue(out var json))
            {
                LastRawSample = json.Length > 400 ? json.Substring(0, 400) : json;
                if (logRawMessages) Debug.Log("[PiWS] " + LastRawSample);

                PiFrame frame = null;
                try
                {
                    frame = JsonUtility.FromJson<PiFrame>(json);
                }
                catch (Exception e)
                {
                    LastError = "JSON 解析失败: " + e.Message;
                    continue;
                }

                if (frame == null || frame.current == null)
                {
                    LastError = "JSON 里没有 current 字段";
                    continue;
                }

                LastMessageTime = Time.unscaledTime;
                FramesReceived++;
                LastError = "";

                try { FrameReceived?.Invoke(frame); } catch (Exception e) { Debug.LogException(e); }
            }
        }

        /// <summary>数据是否还新鲜（默认 5 秒内收到过）。</summary>
        public bool HasFreshData(float timeoutSeconds)
        {
            return LastMessageTime >= 0f && (Time.unscaledTime - LastMessageTime) <= Mathf.Max(0.5f, timeoutSeconds);
        }

        /// <summary>距离上次收到数据的秒数（没收到过返回 -1）。</summary>
        public float SecondsSinceLastMessage
            => LastMessageTime < 0f ? -1f : Time.unscaledTime - LastMessageTime;
    }
}
