import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import './App.css'; // shared keyframes (animate-scaleUp, ...) of the public pages
import './app/setupAxios'; // the access token on every request, refresh on 401
import { restoreSession } from './features/auth/useAuthStore';
import { applyDarkMode, savedDarkMode } from './shared/theme';
import App from './app/App';
import reportWebVitals from './reportWebVitals';

// Light or dark mode from the last visit, before anything renders
applyDarkMode(savedDarkMode(), { remember: false });

// The login, if the refresh cookie still holds one (docs/SAFER_LOGIN_TOKENS_PLAN.md); the app waits for the answer
restoreSession();

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

reportWebVitals();
