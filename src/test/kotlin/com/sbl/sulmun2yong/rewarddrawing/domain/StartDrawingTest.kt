package com.sbl.sulmun2yong.rewarddrawing.domain

import com.sbl.sulmun2yong.global.config.WebDriverConfig
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import kotlin.test.Test

class StartDrawingTest {
    val webDriver = WebDriverConfig().webDriver()

    @Test
    fun `추첨 페이지로 진입한다`() {
        // given

        // when

        // 외부 서비스에서 IP 주소 가져오기
        webDriver.get("https://ip.pe.kr/")

        // 페이지 내용 (IP 주소) 가져오기
        val ipAddress = webDriver.findElement(By.tagName("h1")).text
        println("IP Address is: $ipAddress")

        webDriver.get("http://localhost:3000/s/00363c6a-db22-4df3-b75a-2dd347c8089f/p")
        val fingerprint: Map<String, Any> = getBrowserFingerprint()

        webDriver.close()

        println("Browser Fingerprint: $fingerprint")

        // then
    }

    private fun getBrowserFingerprint(): Map<String, Any> {
        val js = webDriver as JavascriptExecutor
        val fingerprint: MutableMap<String, Any> = HashMap()

        fingerprint["userAgent"] = js.executeScript("return navigator.userAgent;")
        fingerprint["language"] = js.executeScript("return navigator.language;")
        fingerprint["platform"] = js.executeScript("return navigator.platform;")
        fingerprint["screenResolution"] = js.executeScript("return [window.screen.width, window.screen.height];")
        fingerprint["colorDepth"] = js.executeScript("return window.screen.colorDepth;")

        return fingerprint
    }

//    @Test
//    fun `추첨 페이지로 진입한다, 참여하지 않은 설문에 대한 추첨 페이지에 진입한다`() {
//        // given
//
//        // when
//
//        // then
//    }
//
//    @Test
//    fun `추첨 페이지로 진입한다, 이미 참여한 브라우저에서 참여하거나 같은 IP에서 참여한 설문에 대한 추첨 페이지에 진입한다`() {
//        // given
//
//        // when
//
//        // then
//    }
//
//    @Test
//    fun `추첨을 시작한다`() {
//        // given
//        val selectedNumber = 1
//
//        // when
//
//        // then
//    }
//
//    @Test
//    fun `이미 선택한 티켓을 선택한다`() {
//        // given
//        val selectedNumber = 1
//
//        // when
//        val selectedTicket = drawingBoard.selectTicket(selectedNumber)
//        if (selectedTicket.isWinningPosition) {
//            println("당첨")
//        } else {
//            println("꽝")
//        }
//
//        // then
//    }
//
//    @Test
//    fun `마감된 추첨 보드에서 추첨을 시도한다`() {
//        // given
//        val selectedNumber = 1
//
//        // when
//        val selectedTicket = drawingBoard.selectTicket(selectedNumber)
//        if (selectedTicket.isWinningPosition) {
//            println("당첨")
//        } else {
//            println("꽝")
//        }
//
//        // then
//    }
}
