const { spawn } = require('child_process');
const http = require('http');

// Start Next.js dev server
console.log('🚀 Starting Next.js dev server...');
const nextDev = spawn('npm', ['run', 'dev'], {
  stdio: 'inherit',
  shell: true,
});

// Wait for server to be ready
function waitForServer(url, maxAttempts = 30, interval = 1000) {
  return new Promise((resolve, reject) => {
    let attempts = 0;
    
    const check = () => {
      attempts++;
      const req = http.get(url, (res) => {
        if (res.statusCode === 200) {
          console.log('✅ Next.js server is ready!');
          resolve();
        } else {
          if (attempts < maxAttempts) {
            setTimeout(check, interval);
          } else {
            reject(new Error('Server did not become ready in time'));
          }
        }
      });
      
      req.on('error', () => {
        if (attempts < maxAttempts) {
          setTimeout(check, interval);
        } else {
          reject(new Error('Server did not become ready in time'));
        }
      });
      
      req.end();
    };
    
    check();
  });
}

// Wait for server then start Electron
waitForServer('http://localhost:3000')
  .then(() => {
    console.log('🚀 Starting Electron...');
    const electron = spawn('electron', ['.'], {
      stdio: 'inherit',
      shell: true,
    });
    
    electron.on('close', (code) => {
      console.log(`Electron exited with code ${code}`);
      nextDev.kill();
      process.exit(code);
    });
    
    process.on('SIGINT', () => {
      console.log('\n🛑 Shutting down...');
      nextDev.kill();
      electron.kill();
      process.exit(0);
    });
  })
  .catch((error) => {
    console.error('❌ Error:', error.message);
    nextDev.kill();
    process.exit(1);
  });

