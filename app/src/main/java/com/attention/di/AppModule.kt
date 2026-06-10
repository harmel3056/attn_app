package com.attention.di

import android.content.Context
import com.attention.data.AppDatabase
import com.attention.data.dao.ArticleDao
import com.attention.data.dao.DailyBriefingDao
import com.attention.data.dao.SourceDao
import com.attention.data.dao.SummaryDao
import com.attention.data.remote.ClaudeApiService
import com.attention.data.remote.ClaudeRetrofitClient
import com.attention.data.remote.FeedFetcher
import com.attention.data.repository.ClaudeRepository
import com.attention.data.repository.NewsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideClaudeApiService(): ClaudeApiService {
        return ClaudeRetrofitClient.create()
    }

    @Provides
    @Singleton
    fun provideClaudeRepository(
        claudeApiService: ClaudeApiService,
        articleDao: ArticleDao,
        dailyBriefingDao: DailyBriefingDao,
        summaryDao: SummaryDao
    ): ClaudeRepository = ClaudeRepository(claudeApiService, articleDao, dailyBriefingDao, summaryDao)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideArticleDao(database: AppDatabase): ArticleDao = database.articleDao()

    @Provides
    @Singleton
    fun provideSourceDao(database: AppDatabase): SourceDao = database.sourceDao()

    @Provides
    @Singleton
    fun provideSummaryDao(database: AppDatabase): SummaryDao = database.summaryDao()

    @Provides
    @Singleton
    fun provideDailyBriefingDao(database: AppDatabase): DailyBriefingDao = database.dailyBriefingDao()

    @Provides
    @Singleton
    fun provideFeedFetcher(): FeedFetcher = FeedFetcher()

    @Provides
    @Singleton
    fun provideNewsRepository(
        articleDao: ArticleDao,
        sourceDao: SourceDao,
        feedFetcher: FeedFetcher,
        claudeRepository: ClaudeRepository
    ): NewsRepository = NewsRepository(articleDao, sourceDao, feedFetcher, claudeRepository)
}
