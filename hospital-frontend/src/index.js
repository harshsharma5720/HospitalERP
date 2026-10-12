import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import './App.css'; // shared keyframes (animate-scaleUp, ...) of the public pages
import './app/setupAxios'; // global JWT header + 401 handling
import { applyDarkMode, savedDarkMode } from './shared/theme';
import App from './app/App';
import reportWebVitals from './reportWebVitals';

// Light or dark mode from the last visit, before anything renders
applyDarkMode(savedDarkMode(), { remember: false });

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

reportWebVitals();
