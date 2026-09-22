package com.solargridx.app.network

import com.solargridx.app.models.DashboardSummary
import retrofit2.Response
import retrofit2.http.GET

/**
 * DashboardApiService.kt
 * Interface defining the GET /api/dashboard/summary endpoint.
 */
interface DashboardApiService {

    @GET("api/dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummary>
}
