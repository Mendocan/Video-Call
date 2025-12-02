import { WebSocketServer } from 'ws';
import { createServer } from 'http';
import { parse } from 'url';
import { networkInterfaces } from 'os';
import pushService from './services/pushService.js';
// SMS servisi kaldırıldı - SMS doğrulama kullanılmıyor

/**
 * WebRTC Signaling Server
 * 
 * Bu sunucu Wi-Fi ve mobil veri üzerinden telefonları eşleştirir.
 * Room code ile aynı odaya bağlanan telefonlar birbirine SDP ve ICE mesajları gönderir.
 * 
 * ÖZELLİKLER:
 * - ✅ İki kişilik görüşme (1-1)
 * - ✅ Grup görüşme (3+ kişi)
 * - ✅ Wi-Fi üzerinden çalışır
 * - ✅ Mobil veri (operatör internet) üzerinden çalışır
 * - ✅ Tüm eklenen kişilerle görüşme
 * - ✅ Chat mesajları
 * - ✅ Dosya paylaşımı
 * 
 * NASIL ÇALIŞIR:
 * 1. Telefon A: startCall() ile room code oluşturur ve sunucuya bağlanır
 * 2. Telefon B, C, D...: joinCall() ile aynı room code'u kullanarak sunucuya bağlanır
 * 3. Sunucu: Aynı room code'a sahip TÜM telefonları eşleştirir (grup görüşme)
 * 4. Her telefon: Offer/Answer (SDP) ve ICE candidate'ları gönderir
 * 5. Sunucu: Mesajları aynı odadaki TÜM diğer telefonlara iletir
 * 6. WebRTC bağlantısı kurulur, medya doğrudan telefonlar arasında akar (sunucu geçmez)
 * 
 * AĞ DESTEĞİ:
 * - Wi-Fi (ev, işyeri, ortak ağlar)
 * - Mobil veri (4G/5G - operatör internet hizmeti)
 * - WebRTC otomatik olarak en iyi bağlantı yolunu seçer
 */

// Room yönetimi: roomCode -> [WebSocket connections]
const rooms = new Map();

// Connection yönetimi: WebSocket -> { roomCode, phoneNumber, participantId, ... }
const connections = new Map();

// Participant yönetimi: roomCode -> [{ phoneNumber, participantId, name, ... }]
const participants = new Map();

// Kullanıcı kayıt sistemi: phoneNumber -> { ws, phoneNumber, name, connectedAt, lastSeen }
const userRegistry = new Map();

// WebSocket bağlantısı: ws -> phoneNumber (ters mapping)
const wsToPhoneNumber = new Map();

// OTP Store kaldırıldı - SMS doğrulama kullanılmıyor

// Grup yönetimi: groupId -> { name, members[], createdBy, createdAt, roomCode }
const groups = new Map();

// Kullanıcının grupları: phoneNumber -> Set<groupId>
const userGroups = new Map();

// Engellenenler listesi: phoneNumber -> Set<blockedPhoneNumber>
const blockedUsers = new Map();

// HTTP server oluştur (WebSocket upgrade için)
const server = createServer((req, res) => {
  // Ana sayfa (root)
  if (req.url === '/' || req.url === '') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(`
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Video Call Signaling Server</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            min-height: 100vh;
        }
        .container {
            background: rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(10px);
            padding: 40px;
            border-radius: 20px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
        }
        h1 {
            margin: 0 0 10px 0;
            font-size: 2.5em;
        }
        .status {
            display: inline-block;
            background: #4CAF50;
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 0.9em;
            margin-bottom: 30px;
        }
        .endpoint {
            background: rgba(255, 255, 255, 0.2);
            padding: 15px;
            border-radius: 10px;
            margin: 15px 0;
            border-left: 4px solid #00BCD4;
        }
        .endpoint code {
            background: rgba(0, 0, 0, 0.3);
            padding: 5px 10px;
            border-radius: 5px;
            font-family: 'Courier New', monospace;
            display: block;
            margin: 10px 0;
            word-break: break-all;
        }
        .feature {
            display: inline-block;
            background: rgba(255, 255, 255, 0.2);
            padding: 8px 15px;
            border-radius: 20px;
            margin: 5px;
            font-size: 0.9em;
        }
        a {
            color: #00BCD4;
            text-decoration: none;
        }
        a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 Video Call Signaling Server</h1>
        <div class="status">✅ Online</div>
        
        <h2>📡 Endpoints</h2>
        
        <div class="endpoint">
            <strong>Health Check (JSON)</strong>
            <code>GET /health</code>
            <a href="/health" target="_blank">Test Et →</a>
        </div>
        
        <div class="endpoint">
            <strong>Bağlantı Durumu (Test Sayfası)</strong>
            <code>GET /test</code>
            <a href="/test" target="_blank">Bağlı Kullanıcıları Gör →</a>
            <p>Tek telefonla test için: Telefonunuzda uygulamayı açın, bu sayfayı yenileyin, bağlantınızı görün!</p>
        </div>
        
        <div class="endpoint">
            <strong>WebSocket Connection</strong>
            <code>wss://video-call-dyx9.onrender.com/ws</code>
            <p>Android uygulaması bu URL'yi kullanır.</p>
        </div>
        
        <h2>✨ Özellikler</h2>
        <div>
            <span class="feature">✅ Kullanıcı Kaydı</span>
            <span class="feature">✅ Grup Yönetimi</span>
            <span class="feature">✅ Bireysel Arama</span>
            <span class="feature">✅ Grup Araması</span>
            <span class="feature">✅ Wi-Fi Desteği</span>
            <span class="feature">✅ Mobil Veri</span>
            <span class="feature">✅ Chat</span>
            <span class="feature">✅ Dosya Paylaşımı</span>
        </div>
        
        <h2>📊 Server Durumu</h2>
        <p>Detaylı bilgi için: <a href="/health" target="_blank">/health</a></p>
        
        <p style="margin-top: 40px; opacity: 0.8; font-size: 0.9em;">
            Video Call - Private, Secure, Decentralized
        </p>
    </div>
</body>
</html>
    `);
    return;
  }

  // Test sayfası - Bağlı kullanıcıları görmek için
  if (req.url === '/test' || req.url === '/status') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    
    const registeredUsersList = Array.from(userRegistry.keys());
    const connectionsList = Array.from(connections.values()).map(conn => ({
      phoneNumber: conn.phoneNumber || 'Kayıtsız',
      name: conn.name || 'İsimsiz',
      connectedAt: conn.connectedAt,
      isRegistered: conn.isRegistered,
      clientIP: conn.clientIP
    }));
    
    const html = `<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Video Call - Bağlantı Durumu</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; background: #1a1a1a; color: #fff; }
        h1 { color: #4CAF50; }
        .status-card { background: #2d2d2d; border-radius: 8px; padding: 20px; margin: 20px 0; }
        .stat { display: inline-block; margin: 10px 20px 10px 0; padding: 10px 20px; background: #3d3d3d; border-radius: 4px; }
        .stat-value { font-size: 24px; font-weight: bold; color: #4CAF50; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #3d3d3d; }
        th { background: #3d3d3d; color: #4CAF50; }
        .online { color: #4CAF50; }
        .refresh-btn { background: #4CAF50; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; }
    </style>
    <script>setInterval(() => location.reload(), 5000);</script>
</head>
<body>
    <h1>📱 Video Call - Bağlantı Durumu</h1>
    <div class="status-card">
        <h2>📊 Genel Durum</h2>
        <div class="stat"><div class="stat-value">${connections.size}</div><div>Aktif Bağlantı</div></div>
        <div class="stat"><div class="stat-value">${userRegistry.size}</div><div>Kayıtlı Kullanıcı</div></div>
        <div class="stat"><div class="stat-value">${connectionsList.filter(c => c.isRegistered).length}</div><div>Online</div></div>
        <button class="refresh-btn" onclick="location.reload()">🔄 Yenile</button>
    </div>
    <div class="status-card">
        <h2>👥 Bağlı Kullanıcılar</h2>
        <table>
            <tr><th>Telefon</th><th>İsim</th><th>IP</th><th>Durum</th></tr>
            ${connectionsList.length > 0 ? connectionsList.map(conn => 
              `<tr><td>${conn.phoneNumber}</td><td>${conn.name}</td><td>${conn.clientIP}</td><td class="${conn.isRegistered ? 'online' : ''}">${conn.isRegistered ? '✅ Online' : '⏳ Bekliyor'}</td></tr>`
            ).join('') : '<tr><td colspan="4" style="text-align: center;">Henüz bağlı kullanıcı yok</td></tr>'}
        </table>
    </div>
</body>
</html>`;
    
    res.end(html);
    return;
  }
  
  // Health check endpoint
  if (req.url === '/health') {
    let totalParticipants = 0;
    participants.forEach(roomParticipants => {
      totalParticipants += roomParticipants.length;
    });
    
    // Online kullanıcı sayısını hesapla
    let onlineUsers = 0;
    userRegistry.forEach(userInfo => {
      if (userInfo.ws.readyState === 1) {
        onlineUsers++;
      }
    });

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ 
      status: 'ok', 
      timestamp: new Date().toISOString(),
      activeRooms: rooms.size,
      activeConnections: connections.size,
      registeredUsers: userRegistry.size,
      onlineUsers: onlineUsers,
      activeGroups: groups.size,
      totalParticipants: totalParticipants,
      features: {
        userRegistration: true,
        groupManagement: true,
        groupCall: true,
        individualCall: true,
        wifiSupport: true,
        mobileDataSupport: true,
        chat: true,
        fileShare: true
      }
    }));
    return;
  }
  
  res.writeHead(404);
  res.end('Not found');
});

// WebSocket server oluştur
const wss = new WebSocketServer({ 
  server,
  path: '/ws',
  perMessageDeflate: false // Compression devre dışı (düşük latency için)
});

wss.on('connection', (ws, req) => {
  // WebSocket bağlantısından IP adresini al
  const clientIP = req.socket.remoteAddress || 
                   req.headers['x-forwarded-for']?.split(',')[0]?.trim() || 
                   req.headers['x-real-ip'] || 
                   'unknown';
  
  console.log(`[Signaling] Yeni WebSocket bağlantısı: IP=${clientIP}`);

  // Connection bilgilerini kaydet (henüz kayıt olmamış)
  const connectionInfo = {
    roomCode: null,
    phoneNumber: null,
    participantId: null,
    name: null,
    connectedAt: new Date(),
    isRegistered: false,
    clientIP: clientIP, // IP adresini kaydet
    ws
  };
  connections.set(ws, connectionInfo);

  // Mesaj dinle
  ws.on('message', (data) => {
    try {
      const message = JSON.parse(data.toString());
      handleMessage(ws, message);
    } catch (error) {
      console.error('[Signaling] Mesaj parse hatası:', error);
      ws.send(JSON.stringify({ type: 'error', message: 'Invalid message format' }));
    }
  });

  // Bağlantı kapandığında temizle
  ws.on('close', () => {
    console.log(`[Signaling] Bağlantı kapandı`);
    cleanupConnection(ws);
  });

  // Hata durumunda temizle
  ws.on('error', (error) => {
    console.error(`[Signaling] WebSocket hatası:`, error);
    cleanupConnection(ws);
  });

  // Ping/Pong (keep-alive)
  ws.isAlive = true;
  ws.on('pong', () => {
    ws.isAlive = true;
  });
});

// Mesaj işleme
function handleMessage(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo) {
    ws.send(JSON.stringify({ type: 'error', message: 'Connection not found' }));
    return;
  }

  // Register mesajı özel işlenir (kayıt olmadan diğer mesajlar kabul edilmez)
  if (message.type === 'register') {
    handleRegister(ws, message);
    return;
  }

  // OTP request mesajı kaldırıldı - SMS doğrulama kullanılmıyor

  // FCM token kaydetme
  if (message.type === 'register-fcm-token') {
    handleRegisterFCMToken(ws, message);
    return;
  }

  // Kayıt olmamış kullanıcılar sadece register gönderebilir
  if (!connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'error', 
      message: 'Please register first. Send a "register" message with phoneNumber and name.' 
    }));
    return;
  }

  // Room code gerektiren mesajlar için kontrol
  const roomCode = connectionInfo.roomCode;
  if (roomCode) {
    const room = rooms.get(roomCode);
    if (!room) {
      ws.send(JSON.stringify({ type: 'error', message: 'Room not found' }));
      return;
    }
  }

  switch (message.type) {
    case 'offer':
      // Offer'ı aynı odadaki diğer TÜM bağlantılara gönder (grup görüşme desteği)
      // Her katılımcı kendi peer connection'ını yönetir
      if (roomCode) {
        broadcastToRoom(roomCode, ws, message);
      }
      break;

    case 'answer':
      // Answer'ı aynı odadaki diğer TÜM bağlantılara gönder
      if (roomCode) {
        broadcastToRoom(roomCode, ws, message);
      }
      break;

    case 'ice-candidate':
      // ICE candidate'ı aynı odadaki diğer TÜM bağlantılara gönder
      // WebRTC otomatik olarak en iyi bağlantı yolunu seçer (Wi-Fi veya mobil veri)
      if (roomCode) {
        broadcastToRoom(roomCode, ws, message);
      }
      break;

    case 'chat':
      // Kişiye özel chat mi, yoksa room-based chat mi?
      if (message.targetPhoneNumber) {
        // Kişiye özel chat - direkt hedef kullanıcıya gönder
        handleDirectChat(ws, message);
      } else if (roomCode) {
        // Room-based chat - aynı odadaki TÜM katılımcılara gönder
        broadcastToRoom(roomCode, ws, message);
      }
      break;

    case 'file-share':
      // Dosya paylaşımı mesajını aynı odadaki TÜM katılımcılara gönder
      if (roomCode) {
        broadcastToRoom(roomCode, ws, message);
      }
      break;

    case 'get-participants':
      // Odadaki tüm katılımcıları listele
      if (roomCode) {
        const allParticipants = participants.get(roomCode) || [];
        ws.send(JSON.stringify({
          type: 'participants-list',
          participants: allParticipants,
          count: allParticipants.length
        }));
      }
      break;

    case 'leave':
      // Kullanıcı odadan ayrılıyor
      if (roomCode) {
        const leavingConnection = connections.get(ws);
        if (leavingConnection) {
          // Participant listesinden kaldır
          const roomParticipants = participants.get(roomCode) || [];
          const filtered = roomParticipants.filter(p => p.participantId !== leavingConnection.participantId);
          participants.set(roomCode, filtered);
          
          // Diğer kullanıcılara bildir
          broadcastToRoom(roomCode, ws, {
            type: 'participant-left',
            participantId: leavingConnection.participantId,
            participantCount: filtered.length
          });
        }
      }
      cleanupConnection(ws);
      break;

    case 'create-group':
      // Grup oluşturma
      handleCreateGroup(ws, message);
      break;

    case 'join-group':
      // Gruba katılma
      handleJoinGroup(ws, message);
      break;

    case 'leave-group':
      // Gruptan ayrılma
      handleLeaveGroup(ws, message);
      break;

    case 'get-groups':
      // Kullanıcının gruplarını listele
      handleGetGroups(ws);
      break;

    case 'get-group-info':
      // Grup bilgilerini getir
      handleGetGroupInfo(ws, message);
      break;

    case 'call-request':
      // Arama başlatma (bireysel veya grup)
      handleCallRequest(ws, message);
      break;

    case 'call-accept':
      // Arama kabul
      handleCallAccept(ws, message);
      break;

    case 'call-reject':
      // Arama reddetme
      handleCallReject(ws, message);
      break;

    case 'user-status':
      // Kullanıcı durumu sorgulama
      handleUserStatus(ws, message);
      break;

    case 'user-lookup':
      // Kullanıcı lookup (keşif)
      handleUserLookup(ws, message);
      break;

    case 'block-user':
      // Kullanıcı engelleme
      handleBlockUser(ws, message);
      break;

    case 'unblock-user':
      // Kullanıcı engelini kaldırma
      handleUnblockUser(ws, message);
      break;

    case 'get-blocked-users':
      // Engellenenler listesini isteme
      handleGetBlockedUsers(ws);
      break;

    default:
      console.warn(`[Signaling] Bilinmeyen mesaj tipi: ${message.type}`);
  }
}

// Room'daki diğer TÜM bağlantılara mesaj gönder (gönderen hariç)
// Grup görüşme desteği: Mesajlar odadaki tüm katılımcılara iletilir
function broadcastToRoom(roomCode, senderWs, message) {
  const room = rooms.get(roomCode);
  if (!room) return;

  const messageStr = JSON.stringify(message);
  let sentCount = 0;

  room.forEach((client) => {
    // Gönderen hariç, açık olan tüm bağlantılara gönder
    if (client !== senderWs && client.readyState === 1) { // OPEN state
      try {
        client.send(messageStr);
        sentCount++;
      } catch (error) {
        console.error(`[Signaling] Mesaj gönderme hatası:`, error);
      }
    }
  });

  if (sentCount > 0) {
    const participantCount = participants.get(roomCode)?.length || room.length;
    console.log(`[Signaling] Mesaj gönderildi: room=${roomCode}, type=${message.type}, recipients=${sentCount}/${participantCount}`);
  }
}

// Kişiye özel chat mesajı gönderme
function handleDirectChat(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { targetPhoneNumber, message: messageText, senderPhoneNumber, senderName } = message;
  
  if (!targetPhoneNumber) {
    ws.send(JSON.stringify({ 
      type: 'error', 
      message: 'targetPhoneNumber is required for direct chat' 
    }));
    return;
  }

  // Hedef kullanıcıyı bul
  const targetUser = userRegistry.get(targetPhoneNumber);
  
  if (!targetUser) {
    ws.send(JSON.stringify({
      type: 'chat-error',
      reason: 'User not found or offline',
      targetPhoneNumber: targetPhoneNumber
    }));
    return;
  }

  // Hedef kullanıcı online mı?
  if (targetUser.ws.readyState !== 1) {
    ws.send(JSON.stringify({
      type: 'chat-error',
      reason: 'User is offline',
      targetPhoneNumber: targetPhoneNumber
    }));
    return;
  }

  // Engellenenler kontrolü
  const targetBlockedList = blockedUsers.get(targetPhoneNumber);
  if (targetBlockedList && targetBlockedList.has(senderPhoneNumber)) {
    ws.send(JSON.stringify({
      type: 'chat-error',
      reason: 'You are blocked by this user',
      targetPhoneNumber: targetPhoneNumber
    }));
    return;
  }

  // Hedef kullanıcıya mesajı gönder
  try {
    const chatMessage = JSON.stringify({
      type: 'chat',
      message: messageText,
      senderPhoneNumber: senderPhoneNumber,
      senderName: senderName,
      targetPhoneNumber: targetPhoneNumber,
      timestamp: new Date().toISOString()
    });
    
    targetUser.ws.send(chatMessage);
    console.log(`[Signaling] Kişiye özel chat mesajı gönderildi: ${senderPhoneNumber} -> ${targetPhoneNumber}`);
  } catch (error) {
    console.error(`[Signaling] Chat mesajı gönderilemedi:`, error);
    ws.send(JSON.stringify({
      type: 'chat-error',
      reason: 'Failed to send message',
      targetPhoneNumber: targetPhoneNumber
    }));
  }
}

// OTP fonksiyonları kaldırıldı - SMS doğrulama kullanılmıyor

// Telefon numarasını normalize et (backend formatı: 0 ile başlayan)
function normalizePhoneNumber(phoneNumber) {
  if (!phoneNumber) return null;
  
  // Sadece rakamları al
  let cleaned = phoneNumber.replace(/\D/g, '');
  
  // +90 ile başlıyorsa, +90'ı kaldır ve 0 ekle
  if (cleaned.startsWith('90') && cleaned.length === 12) {
    return '0' + cleaned.substring(2);
  }
  
  // 0 ile başlıyorsa olduğu gibi döndür
  if (cleaned.startsWith('0') && cleaned.length === 11) {
    return cleaned;
  }
  
  // Diğer durumlarda (10 haneli veya başka format) olduğu gibi döndür
  // Ama log'a yaz
  if (cleaned.length > 0 && cleaned.length < 10) {
    console.warn(`[Signaling] Uyarı: Telefon numarası formatı beklenmeyen: ${phoneNumber} -> ${cleaned}`);
  }
  
  return cleaned;
}

// Kullanıcı kayıt işleme
function handleRegister(ws, message) {
  const { phoneNumber, name } = message;
  
  if (!phoneNumber) {
    ws.send(JSON.stringify({ 
      type: 'register-error', 
      message: 'phoneNumber is required' 
    }));
    return;
  }

  // Telefon numarasını normalize et
  const normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
  if (!normalizedPhoneNumber) {
    ws.send(JSON.stringify({ 
      type: 'register-error', 
      message: 'Invalid phone number format' 
    }));
    return;
  }

  const connectionInfo = connections.get(ws);
  if (!connectionInfo) {
    ws.send(JSON.stringify({ 
      type: 'register-error', 
      message: 'Connection not found' 
    }));
    return;
  }

  // Eğer bu telefon numarası zaten kayıtlıysa, eski bağlantıyı kapat
  const existingUser = userRegistry.get(normalizedPhoneNumber);
  if (existingUser && existingUser.ws !== ws && existingUser.ws.readyState === 1) {
    console.log(`[Signaling] Telefon numarası ${normalizedPhoneNumber} zaten kayıtlı, eski bağlantı kapatılıyor`);
    existingUser.ws.close(1000, 'New registration from same phone number');
    cleanupConnection(existingUser.ws);
  }

  // Kullanıcıyı kaydet (IP adresi ile birlikte)
  const userInfo = {
    ws,
    phoneNumber: normalizedPhoneNumber, // Normalize edilmiş telefon numarasını kullan
    name: name || null,
    clientIP: connectionInfo.clientIP, // IP adresini ekle
    connectedAt: new Date(),
    lastSeen: new Date()
  };
  
  userRegistry.set(normalizedPhoneNumber, userInfo);
  wsToPhoneNumber.set(ws, normalizedPhoneNumber);
  
  // Connection bilgilerini güncelle
  connectionInfo.phoneNumber = normalizedPhoneNumber;
  connectionInfo.name = name || null;
  connectionInfo.isRegistered = true;
  connectionInfo.participantId = `participant_${Date.now()}`;

  console.log(`[Signaling] Kullanıcı kaydedildi: phoneNumber=${normalizedPhoneNumber} (original: ${phoneNumber}), name=${name || 'N/A'}, IP=${userInfo.clientIP}`);
  console.log(`[Signaling] Kayıtlı kullanıcılar:`, Array.from(userRegistry.keys()));

  // Başarı mesajı gönder
  ws.send(JSON.stringify({
    type: 'registered',
    phoneNumber: normalizedPhoneNumber,
    name: name || null,
    timestamp: new Date().toISOString()
  }));
}

// FCM Token kaydetme
function handleRegisterFCMToken(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'fcm-token-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { fcmToken } = message;
  const phoneNumber = connectionInfo.phoneNumber;
  
  if (!fcmToken) {
    ws.send(JSON.stringify({ 
      type: 'fcm-token-error', 
      message: 'fcmToken is required' 
    }));
    return;
  }

  pushService.registerFCMToken(phoneNumber, fcmToken);
  
  ws.send(JSON.stringify({
    type: 'fcm-token-registered',
    phoneNumber: phoneNumber,
    timestamp: new Date().toISOString()
  }));
  
  console.log(`[Signaling] FCM Token kaydedildi: phoneNumber=${phoneNumber}`);
}

// Grup ID oluşturma
function generateGroupId() {
  return `group_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

// Room code oluşturma (grup için)
function generateRoomCode() {
  return Math.random().toString(36).substring(2, 10).toUpperCase();
}

// Grup oluşturma
function handleCreateGroup(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { groupName, memberPhoneNumbers } = message;
  const creatorPhoneNumber = connectionInfo.phoneNumber;

  if (!groupName) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'groupName is required' 
    }));
    return;
  }

  if (!Array.isArray(memberPhoneNumbers) || memberPhoneNumbers.length === 0) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'memberPhoneNumbers must be a non-empty array' 
    }));
    return;
  }

  // Tüm üyelerin kayıtlı olup olmadığını kontrol et
  const validMembers = [creatorPhoneNumber]; // Oluşturan otomatik üye
  const invalidMembers = [];

  memberPhoneNumbers.forEach(phoneNumber => {
    if (userRegistry.has(phoneNumber)) {
      if (!validMembers.includes(phoneNumber)) {
        validMembers.push(phoneNumber);
      }
    } else {
      invalidMembers.push(phoneNumber);
    }
  });

  // Grup oluştur
  const groupId = generateGroupId();
  const roomCode = generateRoomCode();
  
  const groupInfo = {
    groupId,
    name: groupName,
    members: validMembers,
    createdBy: creatorPhoneNumber,
    createdAt: new Date(),
    roomCode: roomCode
  };

  groups.set(groupId, groupInfo);

  // Her üyeyi userGroups'a ekle
  validMembers.forEach(phoneNumber => {
    if (!userGroups.has(phoneNumber)) {
      userGroups.set(phoneNumber, new Set());
    }
    userGroups.get(phoneNumber).add(groupId);
  });

  // Room oluştur
  if (!rooms.has(roomCode)) {
    rooms.set(roomCode, []);
    participants.set(roomCode, []);
  }

  console.log(`[Signaling] Grup oluşturuldu: groupId=${groupId}, name=${groupName}, members=${validMembers.length}`);

  // Oluşturana başarı mesajı gönder
  ws.send(JSON.stringify({
    type: 'group-created',
    groupId: groupId,
    groupName: groupName,
    roomCode: roomCode,
    members: validMembers,
    invalidMembers: invalidMembers,
    timestamp: new Date().toISOString()
  }));

  // Diğer üyelere grup oluşturuldu bildirimi gönder
  validMembers.forEach(phoneNumber => {
    if (phoneNumber !== creatorPhoneNumber) {
      const memberUser = userRegistry.get(phoneNumber);
      if (memberUser && memberUser.ws.readyState === 1) {
        memberUser.ws.send(JSON.stringify({
          type: 'group-joined',
          groupId: groupId,
          groupName: groupName,
          roomCode: roomCode,
          addedBy: creatorPhoneNumber,
          timestamp: new Date().toISOString()
        }));
      }
    }
  });
}

// Gruba katılma
function handleJoinGroup(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { groupId } = message;
  const phoneNumber = connectionInfo.phoneNumber;

  if (!groupId) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'groupId is required' 
    }));
    return;
  }

  const groupInfo = groups.get(groupId);
  if (!groupInfo) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Group not found' 
    }));
    return;
  }

  // Zaten üye mi?
  if (groupInfo.members.includes(phoneNumber)) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Already a member of this group' 
    }));
    return;
  }

  // Gruba ekle
  groupInfo.members.push(phoneNumber);
  
  // userGroups'a ekle
  if (!userGroups.has(phoneNumber)) {
    userGroups.set(phoneNumber, new Set());
  }
  userGroups.get(phoneNumber).add(groupId);

  console.log(`[Signaling] Kullanıcı gruba katıldı: phoneNumber=${phoneNumber}, groupId=${groupId}`);

  // Katılan kullanıcıya bildir
  ws.send(JSON.stringify({
    type: 'group-joined',
    groupId: groupId,
    groupName: groupInfo.name,
    roomCode: groupInfo.roomCode,
    members: groupInfo.members,
    timestamp: new Date().toISOString()
  }));

  // Diğer üyelere bildir
  groupInfo.members.forEach(memberPhone => {
    if (memberPhone !== phoneNumber) {
      const memberUser = userRegistry.get(memberPhone);
      if (memberUser && memberUser.ws.readyState === 1) {
        memberUser.ws.send(JSON.stringify({
          type: 'group-member-joined',
          groupId: groupId,
          phoneNumber: phoneNumber,
          name: connectionInfo.name,
          memberCount: groupInfo.members.length,
          timestamp: new Date().toISOString()
        }));
      }
    }
  });
}

// Gruptan ayrılma
function handleLeaveGroup(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { groupId } = message;
  const phoneNumber = connectionInfo.phoneNumber;

  if (!groupId) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'groupId is required' 
    }));
    return;
  }

  const groupInfo = groups.get(groupId);
  if (!groupInfo) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Group not found' 
    }));
    return;
  }

  // Üye mi?
  if (!groupInfo.members.includes(phoneNumber)) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Not a member of this group' 
    }));
    return;
  }

  // Gruptan çıkar
  groupInfo.members = groupInfo.members.filter(m => m !== phoneNumber);
  
  // userGroups'tan çıkar
  const userGroupSet = userGroups.get(phoneNumber);
  if (userGroupSet) {
    userGroupSet.delete(groupId);
    if (userGroupSet.size === 0) {
      userGroups.delete(phoneNumber);
    }
  }

  console.log(`[Signaling] Kullanıcı gruptan ayrıldı: phoneNumber=${phoneNumber}, groupId=${groupId}`);

  // Ayrılan kullanıcıya bildir
  ws.send(JSON.stringify({
    type: 'group-left',
    groupId: groupId,
    timestamp: new Date().toISOString()
  }));

  // Diğer üyelere bildir
  groupInfo.members.forEach(memberPhone => {
    const memberUser = userRegistry.get(memberPhone);
    if (memberUser && memberUser.ws.readyState === 1) {
      memberUser.ws.send(JSON.stringify({
        type: 'group-member-left',
        groupId: groupId,
        phoneNumber: phoneNumber,
        name: connectionInfo.name,
        memberCount: groupInfo.members.length,
        timestamp: new Date().toISOString()
      }));
    }
  });

  // Grup boşsa kaldır
  if (groupInfo.members.length === 0) {
    groups.delete(groupId);
    console.log(`[Signaling] Grup kaldırıldı (boş): groupId=${groupId}`);
  }
}

// Kullanıcının gruplarını listele
function handleGetGroups(ws) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const phoneNumber = connectionInfo.phoneNumber;
  const userGroupSet = userGroups.get(phoneNumber) || new Set();
  
  const groupsList = Array.from(userGroupSet).map(groupId => {
    const groupInfo = groups.get(groupId);
    if (!groupInfo) return null;
    
    return {
      groupId: groupInfo.groupId,
      name: groupInfo.name,
      roomCode: groupInfo.roomCode,
      memberCount: groupInfo.members.length,
      createdBy: groupInfo.createdBy,
      createdAt: groupInfo.createdAt.toISOString()
    };
  }).filter(g => g !== null);

  ws.send(JSON.stringify({
    type: 'groups-list',
    groups: groupsList,
    count: groupsList.length
  }));
}

// Grup bilgilerini getir
function handleGetGroupInfo(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { groupId } = message;
  if (!groupId) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'groupId is required' 
    }));
    return;
  }

  const groupInfo = groups.get(groupId);
  if (!groupInfo) {
    ws.send(JSON.stringify({ 
      type: 'group-error', 
      message: 'Group not found' 
    }));
    return;
  }

  // Üye bilgilerini getir
  const membersInfo = groupInfo.members.map(phoneNumber => {
    const userInfo = userRegistry.get(phoneNumber);
    return {
      phoneNumber: phoneNumber,
      name: userInfo?.name || null,
      isOnline: userInfo ? (userInfo.ws.readyState === 1) : false
    };
  });

  ws.send(JSON.stringify({
    type: 'group-info',
    groupId: groupInfo.groupId,
    name: groupInfo.name,
    roomCode: groupInfo.roomCode,
    members: membersInfo,
    memberCount: groupInfo.members.length,
    createdBy: groupInfo.createdBy,
    createdAt: groupInfo.createdAt.toISOString()
  }));
}

// Arama başlatma (bireysel veya grup)
function handleCallRequest(ws, message) {
  console.log(`[Signaling] call-request alındı:`, JSON.stringify(message));
  
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    console.log(`[Signaling] call-request reddedildi: kullanıcı kayıtlı değil`);
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      reason: 'Please register first',
      message: 'Please register first' 
    }));
    return;
  }

  const { targetPhoneNumber, groupId, callerPhoneNumber, callerName } = message;
  const callerPhone = callerPhoneNumber || connectionInfo.phoneNumber;
  // callerName için daha iyi fallback: connectionInfo.name veya callerPhone
  const callerNameValue = callerName || connectionInfo.name || callerPhone;
  
  console.log(`[Signaling] call-request işleniyor: caller=${callerPhone}, callerName=${callerNameValue}, target=${targetPhoneNumber}, groupId=${groupId}`);

  // Grup araması mı, bireysel arama mı?
  if (groupId) {
    handleGroupCallRequest(ws, groupId, callerPhone, callerNameValue);
    return;
  }

  // Bireysel arama
  if (!targetPhoneNumber) {
    console.log(`[Signaling] call-request reddedildi: targetPhoneNumber veya groupId gerekli`);
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      reason: 'targetPhoneNumber or groupId is required',
      message: 'targetPhoneNumber or groupId is required' 
    }));
    return;
  }

  // Telefon numaralarını normalize et
  const normalizedTargetPhoneNumber = normalizePhoneNumber(targetPhoneNumber);
  const normalizedCallerPhone = normalizePhoneNumber(callerPhone);
  
  if (!normalizedTargetPhoneNumber) {
    console.log(`[Signaling] call-request reddedildi: Geçersiz telefon numarası formatı: ${targetPhoneNumber}`);
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      reason: 'Invalid phone number format',
      targetPhoneNumber: targetPhoneNumber
    }));
    return;
  }

  console.log(`[Signaling] Arama isteği: caller=${normalizedCallerPhone} (original: ${callerPhone}), target=${normalizedTargetPhoneNumber} (original: ${targetPhoneNumber})`);

  // Engellenenler kontrolü: Arayan, aranan tarafından engellenmiş mi?
  const targetBlockedList = blockedUsers.get(normalizedTargetPhoneNumber);
  if (targetBlockedList && targetBlockedList.has(normalizedCallerPhone)) {
    console.log(`[Signaling] call-request reddedildi: ${normalizedCallerPhone} -> ${normalizedTargetPhoneNumber} engellenmiş`);
    ws.send(JSON.stringify({
      type: 'call-error',
      reason: 'You are blocked by this user',
      targetPhoneNumber: normalizedTargetPhoneNumber
    }));
    return;
  }

  // Hedef kullanıcıyı bul (normalize edilmiş telefon numarası ile)
  const targetUser = userRegistry.get(normalizedTargetPhoneNumber);
  
  if (!targetUser) {
    console.log(`[Signaling] call-request reddedildi: ${normalizedTargetPhoneNumber} kayıtlı değil veya offline (original: ${targetPhoneNumber})`);
    console.log(`[Signaling] Kayıtlı kullanıcılar (${userRegistry.size} adet):`, Array.from(userRegistry.keys()));
    console.log(`[Signaling] Arayan kullanıcı: ${normalizedCallerPhone} (original: ${callerPhone})`);
    console.log(`[Signaling] Arayan kullanıcı kayıtlı mı? ${userRegistry.has(normalizedCallerPhone)}`);
    
    // Offline kullanıcı için push notification gönder
    if (pushService.isInitialized() && pushService.hasFCMToken(normalizedTargetPhoneNumber)) {
      console.log(`[Signaling] Offline kullanıcı için push notification gönderiliyor: ${normalizedTargetPhoneNumber}`);
      pushService.sendIncomingCallNotification(
        normalizedTargetPhoneNumber,
        normalizedCallerPhone,
        callerNameValue
      ).then(result => {
        if (result.success) {
          console.log(`[Signaling] Push notification gönderildi: ${targetPhoneNumber}`);
          // Offline call queue'ya ekle (gelecekte kullanılabilir)
          // addToOfflineCallQueue(targetPhoneNumber, { callerPhone, callerName: callerNameValue });
        } else {
          console.log(`[Signaling] Push notification gönderilemedi: ${result.error}`);
        }
      });
    }
    
    ws.send(JSON.stringify({
      type: 'call-error',
      reason: 'User not found or offline',
      targetPhoneNumber: normalizedTargetPhoneNumber
    }));
    return;
  }

  // Hedef kullanıcı online mı?
  if (targetUser.ws.readyState !== 1) {
    console.log(`[Signaling] call-request reddedildi: ${normalizedTargetPhoneNumber} WebSocket durumu: ${targetUser.ws.readyState} (1=OPEN)`);
    ws.send(JSON.stringify({
      type: 'call-error',
      reason: 'User is offline',
      targetPhoneNumber: normalizedTargetPhoneNumber
    }));
    return;
  }
  
  console.log(`[Signaling] Hedef kullanıcı bulundu: ${normalizedTargetPhoneNumber} (original: ${targetPhoneNumber}), IP=${targetUser.clientIP}, WebSocket durumu: ${targetUser.ws.readyState}`);

  // Bireysel görüşme için geçici grup oluştur (2 kişilik)
  const tempGroupId = generateGroupId();
  const roomCode = generateRoomCode();
  
  // ÖNEMLİ: members array'ine normalize edilmiş telefon numaralarını ekle
  // Aksi halde handleCallAccept'te userRegistry.get() eşleşmesi yapılamaz
  const tempGroupInfo = {
    groupId: tempGroupId,
    name: `${callerNameValue || normalizedCallerPhone} - ${targetUser.name || normalizedTargetPhoneNumber}`,
    members: [normalizedCallerPhone, normalizedTargetPhoneNumber], // Normalize edilmiş telefon numaraları
    createdBy: normalizedCallerPhone, // Normalize edilmiş telefon numarası
    createdAt: new Date(),
    roomCode: roomCode,
    isTemporary: true // Bireysel görüşme için geçici grup
  };

  groups.set(tempGroupId, tempGroupInfo);

  // Her iki kullanıcıyı da userGroups'a ekle (normalize edilmiş telefon numaraları ile)
  [normalizedCallerPhone, normalizedTargetPhoneNumber].forEach(phoneNumber => {
    if (!userGroups.has(phoneNumber)) {
      userGroups.set(phoneNumber, new Set());
    }
    userGroups.get(phoneNumber).add(tempGroupId);
  });

  // Room oluştur
  if (!rooms.has(roomCode)) {
    rooms.set(roomCode, []);
    participants.set(roomCode, []);
  }

  console.log(`[Signaling] Bireysel arama başlatıldı: caller=${normalizedCallerPhone} (original: ${callerPhone}), target=${normalizedTargetPhoneNumber} (original: ${targetPhoneNumber}), groupId=${tempGroupId}, callerName=${callerNameValue}`);

  // Hedef kullanıcıya gelen arama bildirimi gönder
  try {
    // callerName için daha iyi fallback: targetUser'ın addedContacts'ında arayan kişiyi bul
    // Şimdilik connectionInfo.name veya callerNameValue kullan
    const finalCallerName = callerNameValue || connectionInfo.name || normalizedCallerPhone;
    
    const incomingCallMessage = JSON.stringify({
      type: 'incoming-call',
      groupId: tempGroupId,
      roomCode: roomCode,
      callerPhoneNumber: normalizedCallerPhone, // Normalize edilmiş telefon numarası
      callerName: finalCallerName, // Null olmayan isim
      isGroupCall: false,
      timestamp: new Date().toISOString()
    });
    console.log(`[Signaling] incoming-call mesajı gönderiliyor: ${incomingCallMessage}`);
    console.log(`[Signaling] callerName: ${finalCallerName}, callerPhoneNumber: ${normalizedCallerPhone} (original: ${callerPhone})`);
    targetUser.ws.send(incomingCallMessage);
    console.log(`[Signaling] incoming-call mesajı gönderildi: ${normalizedTargetPhoneNumber} (original: ${targetPhoneNumber})`);
  } catch (error) {
    console.error(`[Signaling] incoming-call mesajı gönderilemedi:`, error);
    ws.send(JSON.stringify({
      type: 'call-error',
      reason: 'Failed to send call notification',
      targetPhoneNumber: targetPhoneNumber
    }));
    return;
  }

  // Arayan kullanıcıya onay mesajı gönder
  ws.send(JSON.stringify({
    type: 'call-request-sent',
    groupId: tempGroupId,
    roomCode: roomCode,
    targetPhoneNumber: normalizedTargetPhoneNumber, // Normalize edilmiş telefon numarası
    timestamp: new Date().toISOString()
  }));
}

// Grup araması başlatma
function handleGroupCallRequest(ws, groupId, callerPhoneNumber, callerName) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const groupInfo = groups.get(groupId);
  if (!groupInfo) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'Group not found' 
    }));
    return;
  }

  // Arayan kullanıcı grup üyesi mi?
  if (!groupInfo.members.includes(callerPhoneNumber)) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'You are not a member of this group' 
    }));
    return;
  }

  const roomCode = groupInfo.roomCode;

  console.log(`[Signaling] Grup araması başlatıldı: groupId=${groupId}, caller=${callerPhoneNumber}, members=${groupInfo.members.length}`);

  // Tüm grup üyelerine (arayan hariç) gelen arama bildirimi gönder
  let notifiedCount = 0;
  groupInfo.members.forEach(memberPhone => {
    if (memberPhone !== callerPhoneNumber) {
      const memberUser = userRegistry.get(memberPhone);
      if (memberUser && memberUser.ws.readyState === 1) {
        memberUser.ws.send(JSON.stringify({
          type: 'incoming-call',
          groupId: groupId,
          roomCode: roomCode,
          callerPhoneNumber: callerPhoneNumber,
          callerName: callerName,
          isGroupCall: true,
          groupName: groupInfo.name,
          memberCount: groupInfo.members.length,
          timestamp: new Date().toISOString()
        }));
        notifiedCount++;
      }
    }
  });

  // Arayan kullanıcıya onay mesajı gönder
  ws.send(JSON.stringify({
    type: 'call-request-sent',
    groupId: groupId,
    roomCode: roomCode,
    notifiedCount: notifiedCount,
    totalMembers: groupInfo.members.length - 1, // Arayan hariç
    timestamp: new Date().toISOString()
  }));
}

// Arama kabul
function handleCallAccept(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { groupId } = message;
  if (!groupId) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'groupId is required' 
    }));
    return;
  }

  const groupInfo = groups.get(groupId);
  if (!groupInfo) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'Group not found' 
    }));
    return;
  }

  const phoneNumber = connectionInfo.phoneNumber;
  const roomCode = groupInfo.roomCode;

  // Room'a bağlan
  connectionInfo.roomCode = roomCode;
  
  if (!rooms.has(roomCode)) {
    rooms.set(roomCode, []);
    participants.set(roomCode, []);
  }
  rooms.get(roomCode).push(ws);

  // Participant listesine ekle
  const roomParticipants = participants.get(roomCode) || [];
  const participantData = {
    participantId: connectionInfo.participantId,
    phoneNumber: phoneNumber,
    name: connectionInfo.name,
    joinedAt: connectionInfo.connectedAt.toISOString()
  };
  roomParticipants.push(participantData);
  participants.set(roomCode, roomParticipants);

  console.log(`[Signaling] Arama kabul edildi: phoneNumber=${phoneNumber}, groupId=${groupId}, roomCode=${roomCode}`);

  // Kabul eden kullanıcıya onay mesajı gönder
  ws.send(JSON.stringify({
    type: 'call-accepted',
    groupId: groupId,
    roomCode: roomCode,
    timestamp: new Date().toISOString()
  }));

  // Diğer grup üyelerine bildir
  // ÖNEMLİ: groupInfo.members artık normalize edilmiş telefon numaraları içeriyor
  groupInfo.members.forEach(memberPhone => {
    // memberPhone zaten normalize edilmiş (0 ile başlayan format)
    if (memberPhone !== phoneNumber) {
      const memberUser = userRegistry.get(memberPhone);
      if (memberUser && memberUser.ws.readyState === 1) {
        // name için fallback: connectionInfo.name veya phoneNumber
        const memberName = connectionInfo.name || phoneNumber;
        console.log(`[Signaling] call-accepted-by gönderiliyor: memberPhone=${memberPhone}, phoneNumber=${phoneNumber}, name=${memberName}`);
        memberUser.ws.send(JSON.stringify({
          type: 'call-accepted-by',
          groupId: groupId,
          roomCode: roomCode,
          phoneNumber: phoneNumber,
          name: memberName,
          timestamp: new Date().toISOString()
        }));
      } else {
        console.log(`[Signaling] call-accepted-by gönderilemedi: memberPhone=${memberPhone} bulunamadı veya offline (userRegistry.has=${userRegistry.has(memberPhone)}, readyState=${memberUser?.ws?.readyState})`);
      }
    }
  });
}

// Arama reddetme
function handleCallReject(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { groupId } = message;
  if (!groupId) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'groupId is required' 
    }));
    return;
  }

  const groupInfo = groups.get(groupId);
  if (!groupInfo) {
    ws.send(JSON.stringify({ 
      type: 'call-error', 
      message: 'Group not found' 
    }));
    return;
  }

  const phoneNumber = connectionInfo.phoneNumber;

  console.log(`[Signaling] Arama reddedildi: phoneNumber=${phoneNumber}, groupId=${groupId}`);

  // Reddeden kullanıcıya onay mesajı gönder
  ws.send(JSON.stringify({
    type: 'call-rejected',
    groupId: groupId,
    timestamp: new Date().toISOString()
  }));

  // Diğer grup üyelerine bildir
  groupInfo.members.forEach(memberPhone => {
    if (memberPhone !== phoneNumber) {
      const memberUser = userRegistry.get(memberPhone);
      if (memberUser && memberUser.ws.readyState === 1) {
        memberUser.ws.send(JSON.stringify({
          type: 'call-rejected-by',
          groupId: groupId,
          phoneNumber: phoneNumber,
          name: connectionInfo.name,
          timestamp: new Date().toISOString()
        }));
      }
    }
  });
}

// Kullanıcı durumu sorgulama
function handleUserStatus(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'user-status-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { phoneNumber } = message;
  if (!phoneNumber) {
    ws.send(JSON.stringify({ 
      type: 'user-status-error', 
      message: 'phoneNumber is required' 
    }));
    return;
  }

  const userInfo = userRegistry.get(phoneNumber);
  if (!userInfo) {
    ws.send(JSON.stringify({
      type: 'user-status',
      phoneNumber: phoneNumber,
      status: 'offline',
      isOnline: false,
      lastSeen: null
    }));
    return;
  }

  // Kullanıcı online mı?
  const isOnline = userInfo.ws.readyState === 1;
  const timeSinceLastSeen = new Date() - new Date(userInfo.lastSeen);
  const PRESENCE_TIMEOUT = 5 * 60 * 1000; // 5 dakika

  // 5 dakikadan fazla süredir yanıt vermiyorsa offline say
  const actuallyOnline = isOnline && timeSinceLastSeen < PRESENCE_TIMEOUT;

  ws.send(JSON.stringify({
    type: 'user-status',
    phoneNumber: phoneNumber,
    name: userInfo.name,
    status: actuallyOnline ? 'online' : 'offline',
    isOnline: actuallyOnline,
    lastSeen: userInfo.lastSeen.toISOString(),
    connectedAt: userInfo.connectedAt.toISOString()
  }));
}

// Kullanıcı lookup (keşif) - "Bu numara kayıtlı mı?" kontrolü
function handleUserLookup(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'lookup-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { phoneNumber } = message;
  if (!phoneNumber) {
    ws.send(JSON.stringify({ 
      type: 'lookup-error', 
      message: 'phoneNumber is required' 
    }));
    return;
  }

  const userInfo = userRegistry.get(phoneNumber);
  
  if (userInfo) {
    // Kullanıcı bulundu
    const isOnline = userInfo.ws.readyState === 1;
    ws.send(JSON.stringify({
      type: 'user-lookup',
      phoneNumber: phoneNumber,
      isRegistered: true,
      name: userInfo.name,
      isOnline: isOnline,
      lastSeen: userInfo.lastSeen.toISOString()
    }));
    console.log(`[Signaling] Kullanıcı lookup: ${phoneNumber} -> Kayıtlı (online: ${isOnline})`);
  } else {
    // Kullanıcı bulunamadı
    ws.send(JSON.stringify({
      type: 'user-lookup',
      phoneNumber: phoneNumber,
      isRegistered: false
    }));
    console.log(`[Signaling] Kullanıcı lookup: ${phoneNumber} -> Kayıtlı değil`);
  }
}

// Kullanıcı engelleme
function handleBlockUser(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'block-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { targetPhoneNumber } = message;
  const phoneNumber = connectionInfo.phoneNumber;

  if (!targetPhoneNumber) {
    ws.send(JSON.stringify({ 
      type: 'block-error', 
      message: 'targetPhoneNumber is required' 
    }));
    return;
  }

  if (targetPhoneNumber === phoneNumber) {
    ws.send(JSON.stringify({ 
      type: 'block-error', 
      message: 'You cannot block yourself' 
    }));
    return;
  }

  // Engellenenler listesine ekle
  if (!blockedUsers.has(phoneNumber)) {
    blockedUsers.set(phoneNumber, new Set());
  }
  blockedUsers.get(phoneNumber).add(targetPhoneNumber);

  console.log(`[Signaling] Kullanıcı engellendi: phoneNumber=${phoneNumber}, blocked=${targetPhoneNumber}`);

  ws.send(JSON.stringify({
    type: 'user-blocked',
    targetPhoneNumber: targetPhoneNumber,
    timestamp: new Date().toISOString()
  }));
}

// Kullanıcı engelini kaldırma
function handleUnblockUser(ws, message) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'block-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const { targetPhoneNumber } = message;
  const phoneNumber = connectionInfo.phoneNumber;

  if (!targetPhoneNumber) {
    ws.send(JSON.stringify({ 
      type: 'block-error', 
      message: 'targetPhoneNumber is required' 
    }));
    return;
  }

  // Engellenenler listesinden çıkar
  const blockedList = blockedUsers.get(phoneNumber);
  if (blockedList) {
    blockedList.delete(targetPhoneNumber);
    if (blockedList.size === 0) {
      blockedUsers.delete(phoneNumber);
    }
  }

  console.log(`[Signaling] Kullanıcı engeli kaldırıldı: phoneNumber=${phoneNumber}, unblocked=${targetPhoneNumber}`);

  ws.send(JSON.stringify({
    type: 'user-unblocked',
    targetPhoneNumber: targetPhoneNumber,
    timestamp: new Date().toISOString()
  }));
}

// Engellenenler listesini getir
function handleGetBlockedUsers(ws) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo || !connectionInfo.isRegistered) {
    ws.send(JSON.stringify({ 
      type: 'block-error', 
      message: 'Please register first' 
    }));
    return;
  }

  const phoneNumber = connectionInfo.phoneNumber;
  const blockedList = blockedUsers.get(phoneNumber) || new Set();
  
  const blockedUsersList = Array.from(blockedList).map(blockedPhoneNumber => {
    const userInfo = userRegistry.get(blockedPhoneNumber);
    return {
      phoneNumber: blockedPhoneNumber,
      name: userInfo?.name || null,
      isOnline: userInfo ? (userInfo.ws.readyState === 1) : false
    };
  });

  ws.send(JSON.stringify({
    type: 'blocked-users-list',
    blockedUsers: blockedUsersList,
    count: blockedUsersList.length
  }));
}

// Bağlantı temizleme
function cleanupConnection(ws) {
  const connectionInfo = connections.get(ws);
  if (!connectionInfo) return;

  const phoneNumber = wsToPhoneNumber.get(ws);
  const roomCode = connectionInfo.roomCode;

  // Kullanıcı kaydını temizle
  if (phoneNumber) {
    const userInfo = userRegistry.get(phoneNumber);
    if (userInfo && userInfo.ws === ws) {
      userRegistry.delete(phoneNumber);
      console.log(`[Signaling] Kullanıcı kaydı temizlendi: phoneNumber=${phoneNumber}`);
    }
    wsToPhoneNumber.delete(ws);
  }

  // Connection bilgisini kaldır
  connections.delete(ws);

  // Kullanıcının tüm gruplarından çıkar
  if (phoneNumber) {
    const userGroupSet = userGroups.get(phoneNumber);
    if (userGroupSet) {
      userGroupSet.forEach(groupId => {
        const groupInfo = groups.get(groupId);
        if (groupInfo) {
          groupInfo.members = groupInfo.members.filter(m => m !== phoneNumber);
          
          // Diğer üyelere bildir
          groupInfo.members.forEach(memberPhone => {
            const memberUser = userRegistry.get(memberPhone);
            if (memberUser && memberUser.ws.readyState === 1) {
              memberUser.ws.send(JSON.stringify({
                type: 'group-member-left',
                groupId: groupId,
                phoneNumber: phoneNumber,
                memberCount: groupInfo.members.length,
                timestamp: new Date().toISOString()
              }));
            }
          });

          // Grup boşsa kaldır
          if (groupInfo.members.length === 0) {
            groups.delete(groupId);
            console.log(`[Signaling] Grup kaldırıldı (boş): groupId=${groupId}`);
          }
        }
      });
      userGroups.delete(phoneNumber);
    }
  }

  // Eğer bir room'a bağlıysa, room'dan kaldır
  if (roomCode) {
    // Participant listesinden kaldır
    if (connectionInfo.participantId) {
      const roomParticipants = participants.get(roomCode) || [];
      const filtered = roomParticipants.filter(p => p.participantId !== connectionInfo.participantId);
      participants.set(roomCode, filtered);
    }

    // Room'dan kaldır
    const room = rooms.get(roomCode);
    if (room) {
      const index = room.indexOf(ws);
      if (index > -1) {
        room.splice(index, 1);
      }

      // Room boşsa kaldır
      if (room.length === 0) {
        rooms.delete(roomCode);
        participants.delete(roomCode);
        console.log(`[Signaling] Room kaldırıldı: ${roomCode}`);
      } else {
        const remainingCount = room.length;
        console.log(`[Signaling] Room ${roomCode}: ${remainingCount} katılımcı kaldı`);
      }
    }
  }
}

// Keep-alive: Her 30 saniyede bir ping gönder
const pingInterval = setInterval(() => {
  wss.clients.forEach((ws) => {
    if (ws.isAlive === false) {
      // Yanıt vermeyen bağlantıyı kapat
      cleanupConnection(ws);
      return ws.terminate();
    }

    ws.isAlive = false;
    ws.ping();
    
    // Kayıtlı kullanıcıların lastSeen'ini güncelle
    const phoneNumber = wsToPhoneNumber.get(ws);
    if (phoneNumber) {
      const userInfo = userRegistry.get(phoneNumber);
      if (userInfo) {
        userInfo.lastSeen = new Date();
      }
    }
  });
}, 30000);

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('[Signaling] SIGTERM alındı, sunucu kapatılıyor...');
  clearInterval(pingInterval);
  wss.close(() => {
    server.close(() => {
      console.log('[Signaling] Sunucu kapatıldı');
      process.exit(0);
    });
  });
});

// Gerçek IP adresini bul
function getLocalIP() {
  const interfaces = networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      // IPv4 ve internal olmayan (loopback değil) adresleri tercih et
      if (iface.family === 'IPv4' && !iface.internal) {
        // Önce 192.168.x.x veya 10.x.x.x gibi local network IP'lerini tercih et
        if (iface.address.startsWith('192.168.') || 
            iface.address.startsWith('10.') || 
            iface.address.startsWith('172.')) {
          return iface.address;
        }
      }
    }
  }
  // Eğer local network IP bulunamazsa, ilk IPv4 adresini döndür
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return 'localhost';
}

// Public IP'yi al (internet üzerinden erişilebilir IP)
async function getPublicIP() {
  try {
    // Ücretsiz IP lookup servisleri
    const services = [
      'https://api.ipify.org?format=json',
      'https://api64.ipify.org?format=json',
      'https://ifconfig.me/ip'
    ];
    
    for (const service of services) {
      try {
        const response = await fetch(service);
        if (service.includes('ipify')) {
          const data = await response.json();
          return data.ip;
        } else {
          const text = await response.text();
          return text.trim();
        }
      } catch (e) {
        continue;
      }
    }
    return null;
  } catch (error) {
    console.warn('[Signaling] Public IP alınamadı:', error.message);
    return null;
  }
}

// Backend API'ye signaling server IP'sini kaydet
async function registerWithBackend(localIP, publicIP, port) {
  const backendUrl = process.env.BACKEND_URL || 'http://localhost:3000';
  
  // Cloudflare Tunnel kullanılıyor mu?
  const useCloudflareTunnel = process.env.USE_CLOUDFLARE_TUNNEL === 'true';
  const cloudflareDomain = process.env.CLOUDFLARE_SIGNALING_DOMAIN || 'signaling.videocall.app';
  const cloudflareApiDomain = process.env.CLOUDFLARE_API_DOMAIN || 'api.videocall.app';
  
  let wsUrl, httpUrl;
  
  if (useCloudflareTunnel) {
    // Cloudflare Tunnel domain'leri kullan (wss:// ve https://)
    wsUrl = `wss://${cloudflareDomain}/ws`; // Secure WebSocket
    httpUrl = `https://${cloudflareApiDomain}`; // HTTPS
    console.log(`🌐 Cloudflare Tunnel kullanılıyor: ${wsUrl}`);
  } else {
    // Normal IP bağlantısı (local network veya port forwarding)
    wsUrl = `ws://${localIP}:${port}/ws`;
    httpUrl = `http://${localIP}:${port}`;
    if (publicIP) {
      // Public IP varsa, hem local hem public URL'leri kaydet
      console.log(`🌐 Public IP mevcut: ${publicIP}`);
    }
  }
  
  try {
    const response = await fetch(`${backendUrl}/api/signaling/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        wsUrl,
        httpUrl,
        localIP,
        publicIP
      })
    });
    
    if (response.ok) {
      const data = await response.json();
      console.log(`✅ Signaling server backend'e kaydedildi: ${data.serverInfo.wsUrl}`);
    } else {
      const error = await response.text();
      console.warn(`⚠️  Backend'e kayıt başarısız: ${error}`);
    }
  } catch (error) {
    console.warn(`⚠️  Backend'e bağlanılamadı (${backendUrl}): ${error.message}`);
    console.log(`ℹ️  Signaling server çalışıyor ancak backend'e kaydedilemedi.`);
  }
}

// Server'ı başlat
const PORT = process.env.SIGNALING_PORT || 8080;
const HOST = process.env.SIGNALING_HOST || '0.0.0.0'; // Tüm interface'lerde dinle
const LOCAL_IP = getLocalIP();

server.listen(PORT, HOST, async () => {
  console.log(`🚀 Signaling Server running on ${HOST}:${PORT}`);
  console.log(`📡 WebSocket endpoint: ws://${LOCAL_IP}:${PORT}/ws`);
  console.log(`💡 Health check: http://${LOCAL_IP}:${PORT}/health`);
  if (HOST === '0.0.0.0') {
    console.log(`ℹ️  Server listening on all interfaces. Use ${LOCAL_IP} for local connections.`);
  }
  
  // Backend API'ye kaydet (async, hata olsa bile devam et)
  const publicIP = await getPublicIP();
  if (publicIP) {
    console.log(`🌐 Public IP: ${publicIP}`);
  }
  await registerWithBackend(LOCAL_IP, publicIP, PORT);
});

export default server;

