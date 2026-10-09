package com.anotherlife.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { AnotherLifeApp() }
 }
}
private val sections = listOf("홈", "대화", "장면", "세계", "설정")
@Composable
fun AnotherLifeApp() {
 var selected by remember { mutableIntStateOf(0) }
 var worldName by remember { mutableStateOf("나의 세계") }
 var npcName by remember { mutableStateOf("첫 번째 인물") }
 var message by remember { mutableStateOf("") }
 val messages = remember { mutableStateListOf<String>() }
 MaterialTheme(colorScheme = darkColorScheme()) {
  Scaffold(
   topBar = { Surface(tonalElevation = 4.dp) { Text("Another Life · 오프라인 알파", modifier = Modifier.fillMaxWidth().padding(20.dp), style = MaterialTheme.typography.titleLarge) } },
   bottomBar = { NavigationBar { sections.forEachIndexed { index, title ->
    NavigationBarItem(selected = selected == index, onClick = { selected = index }, icon = { Text(listOf("⌂","◉","▣","◇","⚙")[index]) }, label = { Text(title) })
   } } }
  ) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    when(selected) {
     0 -> { Text("새로운 삶을 시작하세요", style = MaterialTheme.typography.headlineMedium); Text("세계: $worldName"); Text("등장인물: $npcName"); Text("이 빌드는 UI 검증용입니다. AI 모델은 아직 연결되지 않았습니다.") }
     1 -> { Text("대화", style = MaterialTheme.typography.headlineMedium)
      LazyColumn(Modifier.weight(1f)) { items(messages) { Text(it, modifier = Modifier.padding(vertical = 6.dp)) } }
      OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("메시지") }, modifier = Modifier.fillMaxWidth())
      Button(onClick = { if(message.isNotBlank()) { messages.add("나: " + message); message = "" } }) { Text("기록") }
      Text("로컬 임시 대화 기록 · AI 응답 미구현")
     }
     2 -> { Text("장면", style = MaterialTheme.typography.headlineMedium); Text("장면 생성 기능은 이후 빌드에서 활성화됩니다.") }
     3 -> { Text("세계", style = MaterialTheme.typography.headlineMedium)
      OutlinedTextField(value = worldName, onValueChange = { worldName = it }, label = { Text("세계 이름") })
      OutlinedTextField(value = npcName, onValueChange = { npcName = it }, label = { Text("인물 이름") })
     }
     else -> { Text("설정", style = MaterialTheme.typography.headlineMedium); Text("로컬 AI 엔진: 미설치"); Text("모델: Qwen3 8B GGUF (계획)"); Text("개발 버전 0.1.0-alpha") }
    }
   }
  }
 }
}