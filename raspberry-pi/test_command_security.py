import asyncio
import unittest
from unittest.mock import AsyncMock
from sensor_server import SmartRoomSensorServer, DEFAULT_CONFIG, deepcopy


class CommandSecurityTest(unittest.TestCase):
    def test_direct_client_cannot_control_hardware(self):
        server = SmartRoomSensorServer(deepcopy(DEFAULT_CONFIG))
        server.execute_backend_command = AsyncMock()
        client = AsyncMock()
        asyncio.run(server.handle_client_message(client, '{"type":"COMMAND","command":"open_door"}'))
        server.execute_backend_command.assert_not_called()
        self.assertIn('"status": "error"', client.send.call_args.args[0])

    def test_unsupported_backend_command_returns_error(self):
        server = SmartRoomSensorServer(deepcopy(DEFAULT_CONFIG))
        ack = asyncio.run(server.execute_backend_command({"commandId": "test", "command": "unsupported", "trace_id": "trace"}))
        self.assertEqual("error", ack["status"])
        self.assertEqual("trace", ack["trace_id"])

    def test_failed_output_does_not_change_reported_state(self):
        server = SmartRoomSensorServer(deepcopy(DEFAULT_CONFIG))
        before = deepcopy(server.device_state)
        self.assertFalse(server.set_device_state("light", False))
        self.assertEqual(before, server.device_state)

    def test_climate_mode_turns_off_previous_mode(self):
        from unittest.mock import Mock
        server = SmartRoomSensorServer(deepcopy(DEFAULT_CONFIG))
        server.write_output = Mock(return_value=True)
        server.device_state["heating"] = True
        self.assertTrue(server.apply_command({"command": "set_climate_mode", "value": True, "params": {"mode": "cool"}}))
        self.assertFalse(server.device_state["heating"])
        self.assertTrue(server.device_state["cooling"])


if __name__ == "__main__":
    unittest.main()
