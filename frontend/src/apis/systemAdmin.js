import http from '@/utils/http'

function check(data) {
  if (data === false || (data && typeof data === 'object' && 'code' in data && data.code !== 1)) {
    throw new Error(data?.message || '操作失败，请重试')
  }
  return data
}

export const listUsers = () => http.get('/api/user/all').then(({ data }) => check(data))
export const createUser = user => http.post('/api/user/add', user).then(({ data }) => check(data))
export const updateUser = (id, user) => http.put(`/api/user/update/${id}`, user).then(({ data }) => check(data))
export const deleteUser = id => http.delete(`/api/user/${id}`).then(({ data }) => check(data))
export const queryLogs = query => http.post('/api/v1/logs/query', query).then(({ data }) => check(data).data)
