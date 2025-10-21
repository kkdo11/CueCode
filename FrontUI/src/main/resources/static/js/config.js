let API_BASE;

if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
  // Local development environment
  API_BASE = 'http://localhost:13000/api';
} else {
  // Production environment (e.g., cuecode.kr)
  API_BASE = '/api';
}
