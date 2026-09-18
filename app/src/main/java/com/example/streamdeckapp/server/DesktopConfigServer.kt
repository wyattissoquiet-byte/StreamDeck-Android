package com.example.streamdeckapp.server

import android.util.Log
import com.example.streamdeckapp.model.ActionType
import com.example.streamdeckapp.model.DeckAction
import com.example.streamdeckapp.model.DeckProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class DesktopConfigServer(
    private val port: Int = 8080,
    private val getProfile: () -> DeckProfile?,
    private val updateProfile: (DeckProfile) -> Unit,
    private val triggerAction: (Int) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        thread(start = true, name = "StreamDeckWebServer") {
            try {
                serverSocket = ServerSocket(port)
                Log.d("DesktopConfigServer", "Desktop Configuration Server listening on port $port")
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    thread {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("DesktopConfigServer", "Server socket error", e)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            // Read headers
            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                if (line!!.lowercase().startsWith("content-length:")) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read body if POST
            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(chars, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                String(chars, 0, read)
            } else ""

            when {
                path == "/" || path == "/index.html" -> {
                    sendResponse(output, "200 OK", "text/html", getWebPanelHtml())
                }
                path == "/api/profile" && method == "GET" -> {
                    val profile = getProfile()
                    val json = profile?.let { serializeProfile(it) } ?: "{}"
                    sendResponse(output, "200 OK", "application/json", json)
                }
                path == "/api/profile" && method == "POST" -> {
                    try {
                        val updated = parseProfile(body)
                        if (updated != null) {
                            updateProfile(updated)
                            sendResponse(output, "200 OK", "application/json", """{"status":"ok"}""")
                        } else {
                            sendResponse(output, "400 Bad Request", "application/json", """{"error":"invalid_json"}""")
                        }
                    } catch (e: Exception) {
                        sendResponse(output, "500 Error", "application/json", """{"error":"${e.message}"}""")
                    }
                }
                path.startsWith("/api/trigger") && method == "POST" -> {
                    val slot = path.substringAfter("slot=", "0").substringBefore("&").toIntOrNull() ?: 0
                    triggerAction(slot)
                    sendResponse(output, "200 OK", "application/json", """{"status":"triggered","slot":$slot}""")
                }
                else -> {
                    sendResponse(output, "404 Not Found", "text/plain", "Not Found")
                }
            }

            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendResponse(output: OutputStream, status: String, contentType: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $status\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    fun getServerUrl(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var ip = "127.0.0.1"
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        ip = addr.hostAddress
                        break
                    }
                }
            }
            "http://$ip:$port"
        } catch (e: Exception) {
            "http://localhost:$port"
        }
    }

    private fun serializeProfile(profile: DeckProfile): String {
        val root = JSONObject()
        root.put("id", profile.id)
        root.put("name", profile.name)
        root.put("activePageIndex", profile.activePageIndex)
        root.put("rotationDegrees", profile.rotationDegrees)
        val pagesArray = JSONArray()
        for (page in profile.pages) {
            val pageObj = JSONObject()
            pageObj.put("id", page.id)
            pageObj.put("name", page.name)
            val slotsArray = JSONArray()
            for (action in page.slots) {
                val actionObj = JSONObject()
                actionObj.put("id", action.id)
                actionObj.put("type", action.type.name)
                actionObj.put("label", action.label)
                actionObj.put("backgroundColor", action.backgroundColor)
                actionObj.put("labelColor", action.labelColor)
                actionObj.put("shellCommand", action.shellCommand)
                actionObj.put("targetPackageName", action.targetPackageName)
                actionObj.put("targetAppName", action.targetAppName)
                actionObj.put("targetPageIndex", action.targetPageIndex)
                actionObj.put("extraData", action.extraData)
                actionObj.put("touchX", action.touchX)
                actionObj.put("touchY", action.touchY)
                actionObj.put("touchEndX", action.touchEndX)
                actionObj.put("touchEndY", action.touchEndY)
                actionObj.put("swipeDurationMs", action.swipeDurationMs)
                slotsArray.put(actionObj)
            }
            pageObj.put("slots", slotsArray)
            pagesArray.put(pageObj)
        }
        root.put("pages", pagesArray)
        return root.toString(2)
    }

    private fun parseProfile(jsonStr: String): DeckProfile? {
        return try {
            val root = JSONObject(jsonStr)
            val profileId = root.optString("id", "default_profile")
            val profileName = root.optString("name", "Default Profile")
            val activePageIndex = root.optInt("activePageIndex", 0)
            val rotationDegrees = root.optInt("rotationDegrees", 0)

            val pagesArray = root.optJSONArray("pages") ?: JSONArray()
            val pages = mutableListOf<com.example.streamdeckapp.model.DeckPage>()

            for (i in 0 until pagesArray.length()) {
                val pageObj = pagesArray.getJSONObject(i)
                val pageId = pageObj.optString("id")
                val pageName = pageObj.optString("name", "Page ${i + 1}")
                val slotsArray = pageObj.optJSONArray("slots") ?: JSONArray()
                val slots = MutableList(15) { DeckAction() }

                for (j in 0 until minOf(15, slotsArray.length())) {
                    val actionObj = slotsArray.getJSONObject(j)
                    val typeStr = actionObj.optString("type", "NONE")
                    val actionType = try { ActionType.valueOf(typeStr) } catch (e: Exception) { ActionType.NONE }
                    slots[j] = DeckAction(
                        id = actionObj.optString("id"),
                        type = actionType,
                        label = actionObj.optString("label", ""),
                        labelColor = actionObj.optLong("labelColor", 0xFFFFFFFF),
                        backgroundColor = actionObj.optLong("backgroundColor", 0xFF1C1C24),
                        targetPackageName = actionObj.optString("targetPackageName", ""),
                        targetAppName = actionObj.optString("targetAppName", ""),
                        shellCommand = actionObj.optString("shellCommand", ""),
                        targetPageIndex = actionObj.optInt("targetPageIndex", 0),
                        extraData = actionObj.optString("extraData", ""),
                        touchX = actionObj.optDouble("touchX", 0.5).toFloat(),
                        touchY = actionObj.optDouble("touchY", 0.5).toFloat(),
                        touchEndX = actionObj.optDouble("touchEndX", 0.5).toFloat(),
                        touchEndY = actionObj.optDouble("touchEndY", 0.25).toFloat(),
                        swipeDurationMs = actionObj.optLong("swipeDurationMs", 300L)
                    )
                }
                pages.add(com.example.streamdeckapp.model.DeckPage(id = pageId, name = pageName, slots = slots))
            }
            DeckProfile(id = profileId, name = profileName, pages = pages, activePageIndex = activePageIndex, rotationDegrees = rotationDegrees)
        } catch (e: Exception) {
            null
        }
    }

    private fun getWebPanelHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Stream Deck Desktop Configuration Panel</title>
<style>
  :root {
    --bg-main: #0f111a;
    --bg-card: #181a24;
    --bg-slot: #222533;
    --accent: #7c4dff;
    --accent-hover: #966eff;
    --text: #ffffff;
    --text-muted: #8e95aa;
    --border: #2d3148;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
  body { background: var(--bg-main); color: var(--text); padding: 24px; display: flex; flex-direction: column; align-items: center; min-height: 100vh; }
  .header { width: 100%; max-width: 960px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
  .title { font-size: 24px; font-weight: 700; display: flex; align-items: center; gap: 12px; }
  .status-badge { background: #1b5e20; color: #00e676; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; }
  .main-layout { width: 100%; max-width: 960px; display: grid; grid-template-columns: 260px 1fr; gap: 24px; }
  
  .palette { background: var(--bg-card); border-radius: 14px; padding: 18px; border: 1px solid var(--border); height: fit-content; }
  .palette h3 { font-size: 15px; margin-bottom: 14px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
  .action-chip { background: var(--bg-slot); padding: 10px 14px; border-radius: 8px; margin-bottom: 8px; cursor: grab; font-size: 13px; font-weight: 600; display: flex; align-items: center; justify-content: space-between; border: 1px solid var(--border); transition: all 0.2s; }
  .action-chip:hover { border-color: var(--accent); transform: translateX(4px); }
  
  .deck-container { background: var(--bg-card); border-radius: 14px; padding: 24px; border: 1px solid var(--border); }
  .toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
  .page-select { background: var(--bg-slot); color: var(--text); border: 1px solid var(--border); padding: 8px 16px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; outline: none; }
  .btn { background: var(--accent); color: white; border: none; padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
  .btn:hover { background: var(--accent-hover); }
  .btn-secondary { background: var(--bg-slot); border: 1px solid var(--border); }
  .btn-secondary:hover { background: var(--border); }
  
  .grid-3x5 { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
  .slot { aspect-ratio: 1; background: var(--bg-slot); border-radius: 12px; border: 2px dashed var(--border); display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; position: relative; padding: 8px; text-align: center; transition: all 0.2s; user-select: none; }
  .slot:hover { border-color: var(--accent); transform: scale(1.02); }
  .slot.filled { border-style: solid; border-color: rgba(255,255,255,0.15); box-shadow: 0 4px 12px rgba(0,0,0,0.3); }
  .slot-number { position: absolute; top: 6px; left: 8px; font-size: 10px; color: rgba(255,255,255,0.4); font-weight: 700; }
  .slot-label { font-size: 12px; font-weight: 700; margin-top: 6px; word-break: break-word; }
  .slot-type { font-size: 10px; color: var(--text-muted); text-transform: uppercase; font-weight: 600; }

  /* Modal */
  .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.7); backdrop-filter: blur(4px); align-items: center; justify-content: center; z-index: 1000; }
  .modal-content { background: var(--bg-card); width: 440px; border-radius: 14px; padding: 24px; border: 1px solid var(--border); }
  .modal h2 { font-size: 18px; margin-bottom: 16px; }
  .form-group { margin-bottom: 14px; }
  .form-group label { display: block; font-size: 12px; color: var(--text-muted); font-weight: 600; margin-bottom: 6px; }
  .form-control { width: 100%; background: var(--bg-slot); border: 1px solid var(--border); color: white; padding: 10px 14px; border-radius: 8px; font-size: 14px; outline: none; }
  .form-control:focus { border-color: var(--accent); }
  .modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
</style>
</head>
<body>

<div class="header">
  <div class="title">
    <span>🎛️ Stream Deck Desktop Panel</span>
    <span class="status-badge">CONNECTED</span>
  </div>
  <div>
    <button class="btn btn-secondary" onclick="rotateDeck()">🔄 Rotate</button>
    <button class="btn" onclick="saveToServer()">💾 Save to Deck</button>
  </div>
</div>

<div class="main-layout">
  <div class="palette">
    <h3>Quick Assign Actions</h3>
    <div class="action-chip" onclick="quickAssign('MEDIA_PLAY_PAUSE', 'PLAY/PAUSE', 0xFF004D20)">▶️ Play/Pause</div>
    <div class="action-chip" onclick="quickAssign('MEDIA_NEXT', 'NEXT TRACK', 0xFF003366)">⏭️ Next Track</div>
    <div class="action-chip" onclick="quickAssign('MEDIA_PREV', 'PREV TRACK', 0xFF003366)">⏮️ Prev Track</div>
    <div class="action-chip" onclick="quickAssign('VOLUME_UP', 'VOL +', 0xFF8A3B00)">🔊 Volume Up</div>
    <div class="action-chip" onclick="quickAssign('VOLUME_DOWN', 'VOL -', 0xFF8A3B00)">🔉 Volume Down</div>
    <div class="action-chip" onclick="quickAssign('VOLUME_MUTE_TOGGLE', 'MUTE', 0xFF7A0019)">🔇 Mute Toggle</div>
    <div class="action-chip" onclick="quickAssign('NEXT_PAGE', 'NEXT PAGE', 0xFF4A0072)">➡️ Next Page</div>
    <div class="action-chip" onclick="quickAssign('PREV_PAGE', 'PREV PAGE', 0xFF4A0072)">⬅️ Prev Page</div>
    <div class="action-chip" onclick="quickAssign('SHELL_COMMAND', 'REBOOT', 0xFF004D40)">⚡ Shell Command</div>
  </div>

  <div class="deck-container">
    <div class="toolbar">
      <select id="pageSelect" class="page-select" onchange="changePage(this.value)"></select>
      <div style="font-size: 13px; color: var(--text-muted);">Click any button to configure</div>
    </div>
    <div id="grid" class="grid-3x5"></div>
  </div>
</div>

<!-- Modal -->
<div id="modal" class="modal">
  <div class="modal-content">
    <h2 id="modalTitle">Configure Key</h2>
    <div class="form-group">
      <label>Action Type</label>
      <select id="editType" class="form-control">
        <option value="NONE">None / Empty</option>
        <optgroup label="System & Navigation">
          <option value="SYSTEM_HOME">Go Home</option>
          <option value="SYSTEM_BACK">Back</option>
          <option value="SYSTEM_RECENTS">Recent Apps</option>
          <option value="SYSTEM_NOTIFICATIONS">Notifications</option>
          <option value="SYSTEM_QUICK_SETTINGS">Quick Settings</option>
          <option value="SYSTEM_LOCK_SCREEN">Lock Screen</option>
          <option value="SYSTEM_POWER_DIALOG">Power Menu</option>
          <option value="SYSTEM_SPLIT_SCREEN">Split Screen</option>
          <option value="SYSTEM_SCREENSHOT">Screenshot</option>
        </optgroup>
        <optgroup label="Screen Gestures">
          <option value="SIMULATED_TAP">Simulated Screen Tap</option>
          <option value="SIMULATED_SWIPE">Simulated Screen Swipe</option>
          <option value="SIMULATED_SWIPE_UP">Swipe Up</option>
          <option value="SIMULATED_SWIPE_DOWN">Swipe Down</option>
          <option value="SIMULATED_SWIPE_LEFT">Swipe Left</option>
          <option value="SIMULATED_SWIPE_RIGHT">Swipe Right</option>
        </optgroup>
        <optgroup label="Media & Audio">
          <option value="MEDIA_PLAY_PAUSE">Media Play/Pause</option>
          <option value="MEDIA_PLAY">Media Play</option>
          <option value="MEDIA_PAUSE">Media Pause</option>
          <option value="MEDIA_STOP">Media Stop</option>
          <option value="MEDIA_NEXT">Next Track</option>
          <option value="MEDIA_PREV">Previous Track</option>
          <option value="MEDIA_FAST_FORWARD">Fast Forward</option>
          <option value="MEDIA_REWIND">Rewind</option>
          <option value="VOLUME_UP">Volume Up</option>
          <option value="VOLUME_DOWN">Volume Down</option>
          <option value="VOLUME_MUTE_TOGGLE">Mute Toggle</option>
        </optgroup>
        <optgroup label="Display & Brightness">
          <option value="BRIGHTNESS_UP">Brightness Up</option>
          <option value="BRIGHTNESS_DOWN">Brightness Down</option>
          <option value="SCREEN_OFF">Screen Sleep</option>
        </optgroup>
        <optgroup label="Settings Shortcuts">
          <option value="SETTINGS_BLUETOOTH">Bluetooth Settings</option>
          <option value="SETTINGS_WIFI">Wi-Fi Settings</option>
          <option value="SETTINGS_SOUND">Sound Settings</option>
          <option value="SETTINGS_DISPLAY">Display Settings</option>
          <option value="SETTINGS_DATE_TIME">Date & Time</option>
          <option value="SETTINGS_LOCATION">GPS / Location</option>
          <option value="SETTINGS_APPS">Installed Apps</option>
          <option value="SETTINGS_MAIN">Android Settings</option>
        </optgroup>
        <optgroup label="Tools & Vehicle">
          <option value="TORCH_TOGGLE">Flashlight / Torch</option>
          <option value="OPEN_URL">Open Web URL</option>
          <option value="VOICE_ASSISTANT">Voice Assistant</option>
          <option value="DIAL_PHONE">Dial Phone</option>
          <option value="LAUNCH_APP">Launch App</option>
          <option value="SHELL_COMMAND">Shell Command</option>
        </optgroup>
        <optgroup label="Pages">
          <option value="NEXT_PAGE">Next Page</option>
          <option value="PREV_PAGE">Previous Page</option>
          <option value="GOTO_PAGE">Go To Page</option>
        </optgroup>
      </select>
    </div>
    <div class="form-group">
      <label>Button Label</label>
      <input type="text" id="editLabel" class="form-control" placeholder="Button text...">
    </div>
    <div class="form-group" id="groupExtra">
      <label>URL / Phone / Extra Data</label>
      <input type="text" id="editExtra" class="form-control" placeholder="URL or Phone number...">
    </div>
    <div class="form-group" id="groupPackage">
      <label>App Package Name</label>
      <input type="text" id="editPackage" class="form-control" placeholder="e.g. com.spotify.music">
    </div>
    <div class="form-group" id="groupCmd">
      <label>Shell Command</label>
      <input type="text" id="editCmd" class="form-control" placeholder="e.g. input keyevent 26">
    </div>
    <div class="modal-actions">
      <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
      <button class="btn" onclick="saveSlot()">Apply</button>
    </div>
  </div>
</div>

<script>
let profile = null;
let currentSlot = 0;

async function loadProfile() {
  try {
    const res = await fetch('/api/profile');
    profile = await res.json();
    renderPages();
    renderGrid();
  } catch(e) {
    console.error('Failed to load profile', e);
  }
}

function renderPages() {
  const sel = document.getElementById('pageSelect');
  sel.innerHTML = '';
  if (!profile || !profile.pages) return;
  profile.pages.forEach((p, idx) => {
    const opt = document.createElement('option');
    opt.value = idx;
    opt.text = p.name;
    if (idx === profile.activePageIndex) opt.selected = true;
    sel.appendChild(opt);
  });
}

function renderGrid() {
  const grid = document.getElementById('grid');
  grid.innerHTML = '';
  if (!profile || !profile.pages) return;
  const page = profile.pages[profile.activePageIndex] || profile.pages[0];

  for (let i = 0; i < 15; i++) {
    const action = page.slots[i] || { type: 'NONE', label: '' };
    const div = document.createElement('div');
    div.className = 'slot' + (action.type !== 'NONE' ? ' filled' : '');
    
    // Background color
    if (action.backgroundColor) {
      const hex = '#' + ((action.backgroundColor & 0x00FFFFFF).toString(16).padStart(6, '0'));
      div.style.background = hex;
    }

    div.innerHTML = '<span class="slot-number">#' + (i + 1) + '</span>' +
      '<div class="slot-type">' + action.type.replace(/_/g, ' ') + '</div>' +
      '<div class="slot-label">' + (action.label || '') + '</div>';
    div.onclick = () => openModal(i);
    grid.appendChild(div);
  }
}

function changePage(idx) {
  profile.activePageIndex = parseInt(idx);
  renderGrid();
  saveToServer();
}

function openModal(slotIdx) {
  currentSlot = slotIdx;
  const page = profile.pages[profile.activePageIndex];
  const action = page.slots[slotIdx] || {};
  document.getElementById('modalTitle').innerText = 'Configure Key #' + (slotIdx + 1);
  document.getElementById('editType').value = action.type || 'NONE';
  document.getElementById('editLabel').value = action.label || '';
  document.getElementById('editExtra').value = action.extraData || '';
  document.getElementById('editPackage').value = action.targetPackageName || '';
  document.getElementById('editCmd').value = action.shellCommand || '';
  document.getElementById('modal').style.display = 'flex';
}

function closeModal() {
  document.getElementById('modal').style.display = 'none';
}

function saveSlot() {
  const page = profile.pages[profile.activePageIndex];
  const type = document.getElementById('editType').value;
  const label = document.getElementById('editLabel').value;
  const extra = document.getElementById('editExtra').value;
  const pkg = document.getElementById('editPackage').value;
  const cmd = document.getElementById('editCmd').value;

  page.slots[currentSlot] = {
    ...page.slots[currentSlot],
    type: type,
    label: label,
    extraData: extra,
    targetPackageName: pkg,
    shellCommand: cmd
  };

  closeModal();
  renderGrid();
  saveToServer();
}

function quickAssign(type, label, color) {
  const page = profile.pages[profile.activePageIndex];
  page.slots[currentSlot] = {
    ...page.slots[currentSlot],
    type: type,
    label: label,
    backgroundColor: color
  };
  renderGrid();
  saveToServer();
}

async function rotateDeck() {
  profile.rotationDegrees = ((profile.rotationDegrees || 0) + 90) % 360;
  await saveToServer();
  alert('Deck rotated to ' + profile.rotationDegrees + '°');
}

async function saveToServer() {
  try {
    await fetch('/api/profile', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(profile)
    });
  } catch(e) {
    console.error('Failed saving profile', e);
  }
}

loadProfile();
</script>
</body>
</html>
        """.trimIndent()
    }
}
