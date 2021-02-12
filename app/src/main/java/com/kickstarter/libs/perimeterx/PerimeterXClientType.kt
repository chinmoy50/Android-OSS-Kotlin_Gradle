package com.kickstarter.libs.perimeterx

import com.perimeterx.msdk.PXManager
import okhttp3.Request

interface PerimeterXClientType {
    fun manager(): PXManager?
    fun addHeaderTo(builder: Request.Builder?)
}