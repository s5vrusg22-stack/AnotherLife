package com.anotherlife.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.room.withTransaction
import com.anotherlife.app.ai.CharacterPromptBuilder
import com.anotherlife.app.ai.LlamaRuntime
import com.anotherlife.app.ai.ModelImport
import com.anotherlife.app.data.CharacterMemoryStore
import com.anotherlife.app.data.NpcChatMessage
import com.anotherlife.app.data.NpcCharacterProfile
import com.anotherlife.app.data.NpcMemoryRecord
import com.anotherlife.app.data.SimActorRecord
import com.anotherlife.app.data.SimWorldRecord
import com.anotherlife.app.data.WorldStateStore
import com.anotherlife.app.engine.OfflineWorldEngine
import com.anotherlife.app.engine.WorldActor
import com.anotherlife.app.engine.WorldTick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { AnotherLifeApp() }
 }
}

@Composable
fun AnotherLifeApp() {
 val context = LocalContext.current
 val scope = rememberCoroutineScope()
 val runtime = remember { LlamaRuntime() }
 val worldDb = remember { Room.databaseBuilder(context.applicationContext, WorldStateStore::class.java, "world_state.db").build() }
 val memoryDb = remember {
  Room.databaseBuilder(context.applicationContext, CharacterMemoryStore::class.java, "npc_memory.db")
   .addMigrations(CharacterMemoryStore.MIGRATION_1_2).build()
 }
 DisposableEffect(memoryDb, worldDb) { onDispose { memoryDb.close(); worldDb.close() } }

 var profiles by remember { mutableStateOf<List<NpcCharacterProfile>>(emptyList()) }
 var selectedCharacterId by remember { mutableStateOf("") }
 var characterMenuOpen by remember { mutableStateOf(false) }
 var showCreateCharacter by remember { mutableStateOf(false) }
 var newName by remember { mutableStateOf("") }
 var newRole by remember { mutableStateOf("") }
 var newPersonality by remember { mutableStateOf("") }
 var newBackground by remember { mutableStateOf("") }
 var newSpeechStyle by remember { mutableStateOf("") }
 var modelPath by remember { mutableStateOf(File(context.filesDir, "qwen3-8b.gguf").takeIf { it.exists() }?.absolutePath.orEmpty()) }
 var status by remember { mutableStateOf(if (runtime.nativeAvailable) "Qwen3 8B GGUF 파일을 선택하세요." else "네이티브 AI 라이브러리가 없습니다. ARM64 APK 빌드를 확인하세요.") }
 var busy by remember { mutableStateOf(false) }
 var ready by remember { mutableStateOf(false) }
 var prompt by remember { mutableStateOf("") }
 val history = remember { mutableStateListOf<NpcChatMessage>() }
 val listState = rememberLazyListState()
 val selectedCharacter = profiles.firstOrNull { it.id == selectedCharacterId }

 val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
  if (uri != null) {
   busy = true
   ready = false
   modelPath = ""
   status = "모델 파일 복사 중..."
   scope.launch {
    val result = withContext(Dispatchers.IO) {
     runCatching {
      val destination = File(context.filesDir, "qwen3-8b.gguf")
      val input = requireNotNull(context.contentResolver.openInputStream(uri)) { "모델 파일을 읽을 수 없습니다." }
      ModelImport.install(input, destination)
      destination.absolutePath
     }
    }
    modelPath = result.getOrNull() ?: File(context.filesDir, "qwen3-8b.gguf").takeIf { it.exists() }?.absolutePath.orEmpty()
    status = result.fold({ "모델 복사 완료. 로드 버튼을 누르세요." }, { "복사 실패: " + it.message })
    busy = false
   }
  }
 }

 LaunchedEffect(memoryDb) {
  val existing = withContext(Dispatchers.IO) { memoryDb.characters().all() }
  val available = if (existing.isNotEmpty()) existing else {
   val now = System.currentTimeMillis()
   val defaults = listOf(
    NpcCharacterProfile("starter-minseo", "민서", "오래 알고 지낸 다정한 친구", "섬세하고 따뜻하다. 상대의 작은 변화를 잘 알아차리고, 마음을 열기까지 천천히 신뢰를 쌓는다.", "동네 작은 책방에서 일한다. 조용한 산책과 오래된 이야기, 따뜻한 차를 좋아한다.", "부드럽고 솔직한 말투. 감정을 세심하게 표현하지만 과하게 설명하지 않는다.", now),
    NpcCharacterProfile("starter-harin", "하린", "장난기 많은 활기찬 친구", "호기심이 많고 재치 있다. 먼저 말을 걸고 분위기를 풀지만, 중요한 순간에는 진지하고 의리가 있다.", "새로운 장소와 즉흥적인 계획을 좋아한다. 친구들의 취향과 사소한 약속을 잘 기억한다.", "밝고 경쾌한 말투. 가벼운 농담을 섞되 상대가 힘들 때는 장난을 멈춘다.", now + 1),
    NpcCharacterProfile("starter-doyun", "도윤", "조용하고 사려 깊은 동료", "차분하고 관찰력이 좋다. 쉽게 단정하지 않고 충분히 들은 뒤 자기 생각을 말한다.", "작업실에서 음악을 만든다. 혼자 생각할 시간이 필요하지만 믿는 사람에게는 속내를 털어놓는다.", "짧고 담백하게 말한다. 신뢰가 깊어질수록 조금씩 더 솔직해진다.", now + 2)
   )
   withContext(Dispatchers.IO) { defaults.forEach { memoryDb.characters().save(it) } }
   defaults
  }
  profiles = available
  selectedCharacterId = available.firstOrNull()?.id.orEmpty()
 }

 LaunchedEffect(selectedCharacterId) {
  if (selectedCharacterId.isBlank()) return@LaunchedEffect
  val messages = withContext(Dispatchers.IO) {
   memoryDb.messages().forCharacter("default", selectedCharacterId, 100).asReversed()
  }
  history.clear()
  history.addAll(messages)
 }

 LaunchedEffect(history.size) {
  if (history.isNotEmpty()) listState.animateScrollToItem(history.lastIndex)
 }

 MaterialTheme(colorScheme = darkColorScheme()) {
  Scaffold(topBar = {
   Surface(tonalElevation = 3.dp) {
    Text("Another Life · 완전 오프라인 캐릭터 대화", Modifier.fillMaxWidth().padding(20.dp), style = MaterialTheme.typography.titleLarge)
   }
  }) { insets ->
   Column(
    Modifier.fillMaxSize().padding(insets).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
   ) {
    Text(status, style = MaterialTheme.typography.bodyMedium)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
     Box {
      OutlinedButton(enabled = profiles.isNotEmpty(), onClick = { characterMenuOpen = true }) {
       Text(selectedCharacter?.name ?: "캐릭터 불러오는 중")
      }
      DropdownMenu(expanded = characterMenuOpen, onDismissRequest = { characterMenuOpen = false }) {
       profiles.forEach { profile ->
        DropdownMenuItem(
         text = {
          Column {
           Text(profile.name, fontWeight = FontWeight.SemiBold)
           Text(profile.role, style = MaterialTheme.typography.bodySmall)
          }
         },
         onClick = { selectedCharacterId = profile.id; characterMenuOpen = false }
        )
       }
      }
     }
     OutlinedButton(enabled = !busy, onClick = { showCreateCharacter = !showCreateCharacter }) {
      Text(if (showCreateCharacter) "작성 닫기" else "캐릭터 만들기")
     }
    }

    if (selectedCharacter != null) {
     Text(selectedCharacter.role + " · " + selectedCharacter.personality.take(110), style = MaterialTheme.typography.bodySmall)
    }

    if (showCreateCharacter) {
     Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(newName, { newName = it.take(40) }, label = { Text("이름") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
      OutlinedTextField(newRole, { newRole = it.take(100) }, label = { Text("역할과 관계") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
      OutlinedTextField(newPersonality, { newPersonality = it.take(500) }, label = { Text("성격과 반응 방식") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)
      OutlinedTextField(newBackground, { newBackground = it.take(700) }, label = { Text("배경과 기억할 설정") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)
      OutlinedTextField(newSpeechStyle, { newSpeechStyle = it.take(250) }, label = { Text("말투") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
      Button(enabled = !busy && newName.isNotBlank() && newPersonality.isNotBlank(), onClick = {
       val name = newName.trim()
       if (profiles.any { it.name.equals(name, ignoreCase = true) }) {
        status = "같은 이름의 캐릭터가 이미 있어요."
       } else {
        val profile = NpcCharacterProfile(
         UUID.randomUUID().toString(), name, newRole.trim().ifBlank { "새로운 친구" },
         newPersonality.trim(), newBackground.trim(), newSpeechStyle.trim().ifBlank { "성격에 맞는 자연스러운 말투" },
         System.currentTimeMillis()
        )
        scope.launch {
         withContext(Dispatchers.IO) { memoryDb.characters().save(profile) }
         profiles = withContext(Dispatchers.IO) { memoryDb.characters().all() }
         selectedCharacterId = profile.id
         newName = ""; newRole = ""; newPersonality = ""; newBackground = ""; newSpeechStyle = ""
         showCreateCharacter = false
         status = "새 캐릭터를 저장했어요."
        }
       }
      }) { Text("이 캐릭터 추가") }
     }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Button(enabled = !busy, onClick = { picker.launch(arrayOf("*/*")) }) { Text("GGUF 선택") }
     Button(enabled = !busy && runtime.nativeAvailable && modelPath.isNotBlank(), onClick = {
      busy = true
      status = "Qwen3 8B 모델 로딩 중..."
      scope.launch {
       val result = runtime.loadModel(modelPath)
       status = result.fold({ ready = true; "AI 준비 완료 · 컨텍스트 16,384" }, { ready = false; "로드 실패: " + it.message })
       busy = false
      }
     }) { Text("모델 로드") }
    }

    LazyColumn(
     Modifier.weight(1f).fillMaxWidth(),
     state = listState,
     verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
     items(history, key = { it.id }) { message ->
      val player = message.role == "PLAYER"
      Row(Modifier.fillMaxWidth(), horizontalArrangement = if (player) Arrangement.End else Arrangement.Start) {
       Surface(
        color = if (player) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
       ) {
        Column(Modifier.widthIn(max = 330.dp).padding(12.dp)) {
         Text(if (player) "나" else selectedCharacter?.name ?: "캐릭터", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
         Text(message.text, style = MaterialTheme.typography.bodyLarge)
        }
       }
      }
     }
    }

    OutlinedTextField(
     prompt,
     { prompt = it.take(4500) },
     label = { Text("캐릭터에게 말하기") },
     modifier = Modifier.fillMaxWidth(),
     minLines = 1,
     maxLines = 3
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Button(enabled = !busy && ready && selectedCharacter != null && prompt.isNotBlank(), onClick = {
      val active = selectedCharacter ?: return@Button
      val userText = prompt.trim().take(4500)
      if (userText.isBlank()) return@Button
      prompt = ""
      busy = true
      status = active.name + "가 생각 중..."
      val userRecord = NpcChatMessage(UUID.randomUUID().toString(), "default", active.id, "PLAYER", userText, System.currentTimeMillis())
      history.add(userRecord)
      scope.launch {
       withContext(Dispatchers.IO) { memoryDb.messages().save(userRecord) }
       val response = runCatching {
        val memories = withContext(Dispatchers.IO) { memoryDb.memories().forCharacter("default", active.id) }
        val recent = withContext(Dispatchers.IO) {
         memoryDb.messages().forCharacter("default", active.id, 16)
          .asReversed().filterNot { it.id == userRecord.id }
        }
        val fullPrompt = CharacterPromptBuilder.build("default", active, memories, recent, userText)
        runtime.generate(fullPrompt).getOrThrow()
       }
       response.fold(
        { answer ->
         val characterRecord = NpcChatMessage(UUID.randomUUID().toString(), "default", active.id, "CHARACTER", answer, System.currentTimeMillis())
         history.add(characterRecord)
         val saved = withContext(Dispatchers.IO) {
          runCatching {
           memoryDb.messages().save(characterRecord)
           memoryDb.memories().save(NpcMemoryRecord(
            UUID.randomUUID().toString(), "default", active.id,
            "플레이어: " + userText.take(500) + " / " + active.name + ": " + answer.take(700),
            "DIRECT", 55, characterRecord.createdAt
           ))
          }
         }
         status = if (saved.isSuccess) "대화 저장 완료 · " + active.name + "의 기억에 반영됨" else "응답은 완료됐지만 기록 저장에 실패했어요."
        },
        { status = "추론 오류: " + it.message }
       )
       busy = false
      }
     }) { Text("보내기") }
     OutlinedButton(enabled = busy, onClick = { runtime.cancel() }) { Text("중단") }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     OutlinedButton(enabled = !busy, onClick = {
      busy = true
      scope.launch {
       val outcome = runCatching {
        withContext(Dispatchers.IO) {
         worldDb.withTransaction {
          val dao = worldDb.worlds()
          val world = dao.world("default") ?: SimWorldRecord("default")
          val stored = dao.actors("default")
          val actors = if (stored.isEmpty()) listOf(WorldActor("main")) else stored.map {
           WorldActor(it.actorId, it.energy, it.hunger, it.social)
          }
          val next = OfflineWorldEngine.advance(WorldTick(world.minute, actors, emptyList()), 60)
          dao.saveWorld(SimWorldRecord("default", next.minute))
          dao.saveActors(next.actors.map { SimActorRecord("default", it.id, it.energy, it.hunger, it.social) })
          next
         }
        }
       }
       status = outcome.fold({ "세계 시간: " + it.minute + "분 · NPC " + it.actors.size + "명" }, { "세계 진행 실패: " + it.message })
       busy = false
      }
     }) { Text("세계 시간 1시간 진행") }
     Text("로컬 추론 · temperature 0.7 · context 16,384", style = MaterialTheme.typography.bodySmall)
    }
   }
  }
 }
}
