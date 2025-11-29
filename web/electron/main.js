const { app, BrowserWindow, ipcMain, Notification } = require('electron');
const path = require('path');
const { spawn } = require('child_process');

let mainWindow = null;
let nextServer = null;

const isDev = process.env.NODE_ENV === 'development' || !app.isPackaged;

function createWindow() {
  const { screen } = require('electron');
  const primaryDisplay = screen.getPrimaryDisplay();
  const { width, height } = primaryDisplay.workAreaSize;

  mainWindow = new BrowserWindow({
    width: Math.min(width, 1920),
    height: Math.min(height, 1080),
    minWidth: 1200,
    minHeight: 800,
    backgroundColor: '#0f172a', // gray-900
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      enableRemoteModule: false,
    },
    icon: path.join(__dirname, '../public/logo.png'),
    titleBarStyle: process.platform === 'darwin' ? 'hiddenInset' : 'default',
    show: false, // Don't show until ready
  });

  // Show window when ready
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    if (isDev) {
      mainWindow.webContents.openDevTools();
    }
  });

  // Load the app
  if (isDev) {
    // Development: Load from Next.js dev server
    mainWindow.loadURL('http://localhost:3000');
  } else {
    // Production: Load from built Next.js app
    mainWindow.loadFile(path.join(__dirname, '../out/index.html'));
  }

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function startNextServer() {
  if (isDev) {
    // In development, Next.js dev server should be running separately
    return;
  }

  // In production, start Next.js server
  const nextPath = path.join(__dirname, '../.next/standalone');
  const serverPath = path.join(nextPath, 'server.js');
  
  nextServer = spawn('node', [serverPath], {
    cwd: nextPath,
    env: {
      ...process.env,
      PORT: '3000',
      NODE_ENV: 'production',
    },
  });

  nextServer.stdout.on('data', (data) => {
    console.log(`Next.js: ${data}`);
  });

  nextServer.stderr.on('data', (data) => {
    console.error(`Next.js: ${data}`);
  });

  nextServer.on('close', (code) => {
    console.log(`Next.js server exited with code ${code}`);
  });
}

// IPC Handlers
ipcMain.handle('get-app-version', () => {
  return app.getVersion();
});

ipcMain.handle('get-platform', () => {
  return process.platform;
});

ipcMain.handle('show-notification', (event, title, body, options = {}) => {
  if (Notification.isSupported()) {
    const notification = new Notification({
      title,
      body,
      icon: path.join(__dirname, '../public/logo.png'),
      ...options,
    });
    notification.show();
    return true;
  }
  return false;
});

app.whenReady().then(() => {
  createWindow();
  startNextServer();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    if (nextServer) {
      nextServer.kill();
    }
    app.quit();
  }
});

app.on('before-quit', () => {
  if (nextServer) {
    nextServer.kill();
  }
});

