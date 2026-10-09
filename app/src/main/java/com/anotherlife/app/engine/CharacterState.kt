package com.anotherlife.app.engine

/** Offline world state; actor-local knowledge prevents accidental omniscience. */
data class CharacterState(
 val id:String,
 val name:String,
 val mood:Int=50,
 val energy:Int=70,
 val relationships:Map<String,Int> = emptyMap(),
 val memories:List<CharacterMemory> = emptyList(),
 val goals:List<String> = emptyList()
)
data class CharacterMemory(val text:String,val source:String,val importance:Int,val timestamp:Long)
data class AutonomousAction(val actorId:String,val description:String,val reason:String)
class CharacterSimulation {
 fun updateMood(state:CharacterState, delta:Int)=state.copy(mood=(state.mood+delta).coerceIn(0,100))
 fun relate(state:CharacterState,other:String,delta:Int)=state.copy(relationships=state.relationships+(other to ((state.relationships[other]?:50)+delta).coerceIn(0,100)))
 fun remember(state:CharacterState, text:String,source:String,importance:Int,at:Long)=state.copy(memories=(state.memories+CharacterMemory(text,source,importance.coerceIn(0,100),at)).takeLast(200))
 fun decide(state:CharacterState):AutonomousAction=when {
  state.energy<30->AutonomousAction(state.id,"휴식한다","에너지 부족")
  state.mood<25->AutonomousAction(state.id,"조용한 장소로 이동한다","감정 회복")
  state.goals.isNotEmpty()->AutonomousAction(state.id,"목표에 필요한 일을 한다",state.goals.first())
  else->AutonomousAction(state.id,"주변을 살펴본다","자율적인 일상")
 }
 fun privateContext(state:CharacterState):String=buildString{
  appendLine("당신은 ${state.name}입니다. 플레이어를 대신 조종하지 마세요.")
  appendLine("현재 기분 ${state.mood}/100, 에너지 ${state.energy}/100")
  appendLine("당신에게 알려진 기억만 사용하세요:")
  state.memories.sortedByDescending{it.importance}.take(12).forEach{appendLine("- [${it.source}] ${it.text.take(250)}")}
 }
}
