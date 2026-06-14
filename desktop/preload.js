// preload.js
const { contextBridge, ipcRenderer } = require('electron');

// Expose a safe context bridge API to the React application
contextBridge.exposeInMainWorld('electronAPI', {
  platform: process.platform,
  // We can add more handlers here if our frontend needs to call desktop APIs
});
