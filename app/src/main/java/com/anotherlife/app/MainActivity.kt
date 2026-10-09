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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anotherlife.app.ai.LlamaRuntime
import kotlinx.coroutines.launch
import java.io.File
import androidx.room.Room
import com.anotherlife.app.data.CharacterMemoryStore
import com.anotherlife.app.data.NpcMemoryRecord
import com.anotherlife.app.ai.CharacterPromptBuilder
import java.util.UUID

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { AnotherLifeApp() }
 }
}
@Composable
fun AnotherLifeApp() {
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 val runtime=remember { LlamaRuntime() }
 val memoryDb=remember { Room.databaseBuilder(context.applicationContext,CharacterMemoryStore::class.java,"npc_memory.db").build() }
 var characterId by remember { mutableStateOf("main") }
 var modelPath by remember { mutableStateOf("") }
 var status by remember { mutableStateOf("Qwen3 8B GGUF 파일을 선택하세요.") }
 var busy by remember { mutableStateOf(false) }
 var ready by remember { mutableStateOf(false) }
 var prompt by remember { mutableStateOf("") }
 val history=remember { mutableStateListOf<String>() }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->
  if(uri!=null){busy=true;ready=false;modelPath="";status="모델 파일 복사 중..."
   scope.launch {
    val result=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
     runCatching {
      val destination=File(context.filesDir,"qwen3-8b.gguf")
      val input=requireNotNull(context.contentResolver.openInputStream(uri)){"모델 파일을 읽을 수 없습니다."}
      com.anotherlife.app.ai.ModelImport.install(input,destination)
      destination.absolutePath
     }
    }
    modelPath=result.getOrNull()?:""
    status=result.fold({"모델 복사 완료. 로드 버튼을 누르세요."},{"복사 실패: ${it.message}"})
    busy=false
   }
  }
 }
 MaterialTheme(colorScheme=darkColorScheme()){
  Scaffold(topBar={Surface(tonalElevation=3.dp){Text("Another Life · 오프라인 AI",Modifier.fillMaxWidth().padding(20.dp),style=MaterialTheme.typography.titleLarge)}}){insets->
   Column(Modifier.fillMaxSize().padding(insets).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text(status)
    OutlinedTextField(characterId,{characterId=it},label={Text("캐릭터 ID")},modifier=Modifier.fillMaxWidth(),singleLine=true)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Button(enabled=!busy,onClick={picker.launch(arrayOf("*/*"))}){Text("GGUF 선택")}
     Button(enabled=!busy&&modelPath.isNotBlank(),onClick={
      busy=true;status="모델 로딩 중..."
      scope.launch {
       val result=runtime.loadModel(modelPath)
       status=result.fold({ready=true;"AI 모델 준비 완료"},{ready=false;"로드 실패: ${it.message}"})
       busy=false
      }
     }){Text("모델 로드")}
    }
    LazyColumn(Modifier.weight(1f)){items(history){Text(it,Modifier.padding(vertical=7.dp))}}
    OutlinedTextField(prompt,{prompt=it},label={Text("캐릭터에게 말하기")},modifier=Modifier.fillMaxWidth())
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Button(enabled=!busy&&ready&&prompt.isNotBlank(),onClick={
      val userText=prompt;val actor=characterId.trim();prompt="";history.add("나: $userText");busy=true;status="AI 응답 생성 중..."
      scope.launch {
       val result=runCatching {
        require(actor.isNotBlank()){"캐릭터 ID를 입력하세요."}
        val memories=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
         memoryDb.memories().forCharacter("default",actor)
        }
        val fullPrompt=CharacterPromptBuilder.build("default",actor,actor,memories,userText)
        runtime.generate(fullPrompt).getOrThrow()
       }
       result.fold({
        history.add("AI: $it")
        val reply=it
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
         runCatching {
          memoryDb.memories().save(NpcMemoryRecord(UUID.randomUUID().toString(),"default",actor,
           "플레이어: ${userText.take(500)} / 응답: ${reply.take(700)}","DIRECT",55,System.currentTimeMillis()))
         }
        }
        status="준비 완료"
       },{status="추론 오류: ${it.message}"})
       busy=false
      }
     }){Text("보내기")}
     OutlinedButton(enabled=busy,onClick={runtime.cancel()}){Text("중단")}
    }
    Text("완전 오프라인 · 온도 0.7 · 최대 컨텍스트 16384",style=MaterialTheme.typography.bodySmall)
   }
  }
 }
}
