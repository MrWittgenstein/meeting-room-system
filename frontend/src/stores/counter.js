import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import axios from 'axios'

export const useCounterStore = defineStore('counter', () => {
  // 定义一个计数器状态，初始值为0
  const count = ref(0)
  // 计算属性
  const doubleCount=computed(()=>count.value*2)
  // 定义一个方法用于增加计数器的值
  const increment = () => {
    count.value++
  }
  
  const list = ref([])
  const getList = async () => {
    const res = await axios.get('http://test-cn.your-api-server.com/user/register')
    list.value = res.data
  }
  // 返回计数器状态、计算属性和方法
  return {
    count,
    doubleCount,
    increment,
    list,
    getList
  }
})