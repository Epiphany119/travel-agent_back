import axios from 'axios';
const instance = axios.create({
    baseURL: '/api',
    timeout: 60000
});
instance.interceptors.response.use((response) => response.data, (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
});
export default instance;
//# sourceMappingURL=index.js.map