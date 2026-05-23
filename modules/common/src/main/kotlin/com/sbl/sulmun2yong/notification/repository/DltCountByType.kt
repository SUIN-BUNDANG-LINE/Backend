package com.sbl.sulmun2yong.notification.repository

interface DltCountByType {
    fun getNotificationType(): String

    fun getCount(): Long
}
