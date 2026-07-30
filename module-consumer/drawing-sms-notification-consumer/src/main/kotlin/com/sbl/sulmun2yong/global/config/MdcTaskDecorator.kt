package com.sbl.sulmun2yong.global.config

import org.slf4j.MDC
import org.springframework.core.task.TaskDecorator

class MdcTaskDecorator : TaskDecorator {
    override fun decorate(runnable: Runnable): Runnable {
        val contextMap = MDC.getCopyOfContextMap()

        return Runnable {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap)
                }
                runnable.run()
            } finally {
                MDC.clear()
            }
        }
    }
}
