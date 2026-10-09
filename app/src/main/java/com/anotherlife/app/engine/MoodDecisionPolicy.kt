package com.anotherlife.app.engine

/** Pure decision policy; no database, model or Android dependencies. */
data class MoodSnapshot(val joy: Int = 50, val anxiety: Int = 50, val anger: Int = 50,
    val sadness: Int = 50, val relief: Int = 50)

enum class MoodAction { REST, SEEK_SAFETY, TAKE_SPACE, SELF_CARE, SOCIALIZE, PURSUE_GOAL, NONE }

data class MoodDecision(val action: MoodAction, val reason: String, val priority: Int)

object MoodDecisionPolicy {
    fun decide(mood: MoodSnapshot, rest: Int, security: Int, social: Int, autonomy: String): MoodDecision {
        if (autonomy == "OFF") return MoodDecision(MoodAction.NONE, "자율 행동 꺼짐", 0)
        if (rest < 25) return MoodDecision(MoodAction.REST, "휴식 욕구 부족", 100)
        if (security < 25 || mood.anxiety >= 78) return MoodDecision(MoodAction.SEEK_SAFETY, "불안 또는 안전 욕구", 90)
        if (mood.anger >= 75) return MoodDecision(MoodAction.TAKE_SPACE, "분노를 가라앉힐 시간", 85)
        if (mood.sadness >= 76 && mood.joy < 45) return MoodDecision(MoodAction.SELF_CARE, "정서적 회복", 80)
        if (social < 45 && mood.anger < 65 && mood.anxiety < 70 && rest >= 35)
            return MoodDecision(MoodAction.SOCIALIZE, "사회적 욕구와 현재 감정", 65)
        if (mood.joy >= 75 && social < 80 && mood.anger < 60 && rest >= 35)
            return MoodDecision(MoodAction.SOCIALIZE, "기분이 좋아 교류하고 싶음", 55)
        if (mood.relief >= 65 && rest >= 40) return MoodDecision(MoodAction.PURSUE_GOAL, "안정된 감정", 35)
        return MoodDecision(MoodAction.NONE, "긴급 행동 필요 없음", 0)
    }

    fun acceptsContact(mood: MoodSnapshot, rest: Int, trust: Int, affinity: Int, blocked: Boolean): Boolean =
        !blocked && affinity >= 20 && rest >= 25 && trust >= 30 && mood.anger < 75 && mood.anxiety < 82
}
