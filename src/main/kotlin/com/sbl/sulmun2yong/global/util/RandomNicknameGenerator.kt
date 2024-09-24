package com.sbl.sulmun2yong.global.util

import kotlin.random.Random

class RandomNicknameGenerator {
    companion object {
        fun generate(): String {
            val positiveAdjective = getRandomPositiveAdjective()
            val animalName = getRandomAnimalName()
            return "$positiveAdjective $animalName"
        }

        private fun getRandomPositiveAdjective(): String {
            val randomIndex = Random.nextInt(positiveAdjectives.size)
            return positiveAdjectives[randomIndex]
        }

        private fun getRandomAnimalName(): String {
            val randomIndex = Random.nextInt(animalNames.size)
            return animalNames[randomIndex]
        }

        private val positiveAdjectives =
            listOf(
                "멋진",
                "아름다운",
                "사랑스러운",
                "기분 좋은",
                "활기찬",
                "행복한",
                "용감한",
                "현명한",
                "똑똑한",
                "친절한",
                "따뜻한",
                "차분한",
                "건강한",
                "잘생긴",
                "매력적인",
                "유쾌한",
                "긍정적인",
                "발랄한",
                "자유로운",
                "훌륭한",
                "빛나는",
                "깨끗한",
                "상쾌한",
                "풍부한",
                "고운",
                "순수한",
                "평온한",
                "친절한",
                "재치 있는",
                "유머러스한",
                "사려 깊은",
                "활발한",
                "열정적인",
                "끈기 있는",
                "적극적인",
                "신나는",
                "흥미로운",
                "놀라운",
                "기대되는",
                "즐거운",
                "기쁜",
                "자랑스러운",
                "감동적인",
                "성실한",
                "충실한",
                "헌신적인",
                "열정있는",
            )

        private val animalNames =
            listOf(
                "고양이",
                "강아지",
                "코끼리",
                "기린",
                "사자",
                "호랑이",
                "곰",
                "늑대",
                "여우",
                "토끼",
                "사슴",
                "원숭이",
                "판다",
                "코알라",
                "캥거루",
                "말",
                "소",
                "양",
                "염소",
                "닭",
                "오리",
                "거위",
                "공작",
                "앵무새",
                "독수리",
                "매",
                "올빼미",
                "참새",
                "비둘기",
                "까마귀",
                "까치",
                "두루미",
                "황새",
                "치타",
                "하마",
                "악어",
                "두더지",
                "수달",
                "다람쥐",
                "너구리",
                "사막여우",
                "라쿤",
                "뱀",
                "도마뱀",
                "거북이",
                "펭귄",
                "돌고래",
                "용",
                "표범",
                "고래",
                "부엉이",
                "타조",
                "코뿔소",
                "물개",
                "고슴도치",
                "카멜레온",
            )
    }
}
