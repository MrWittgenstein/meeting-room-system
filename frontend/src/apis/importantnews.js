import httpInstance from '@/utils/http'

const getToken = () => localStorage.getItem('token')

export function getImportantNewsAll() {
  return httpInstance({
    url: '/importantnews/all',
    method: 'get',
    headers: { 'Authorization': `Bearer ${getToken()}` }
  })
}

export function getImportantNewsValid() {
  return httpInstance({
    url: '/importantnews/status/valid',
    method: 'get',
    headers: { 'Authorization': `Bearer ${getToken()}` }
  })
}

export function getImportantNewsPriority() {
  return httpInstance({
    url: '/importantnews/priority',
    method: 'get',
    headers: { 'Authorization': `Bearer ${getToken()}` }
  })
}

export function addImportantNews(data) {
  return httpInstance({
    url: '/importantnews/add',
    method: 'post',
    headers: { 'Authorization': `Bearer ${getToken()}` },
    data
  })
}

export function updateImportantNews(data) {
  return httpInstance({
    url: '/importantnews/update',
    method: 'put',
    headers: { 'Authorization': `Bearer ${getToken()}` },
    data
  })
}

export function deleteImportantNews(id) {
  return httpInstance({
    url: `/importantnews/${id}`,
    method: 'delete',
    headers: { 'Authorization': `Bearer ${getToken()}` }
  })
}

export function uploadImportantNewsPicture({ id, file }) {
  const formData = new FormData()
  formData.append('file', file)
  return httpInstance({
    url: '/importantnews/uploadPicture',
    method: 'post',
    headers: {
      'Authorization': `Bearer ${getToken()}`,
      'Content-Type': 'multipart/form-data'
    },
    params: { id },
    data: formData
  })
}
