package eu.kanade.tachiyomi.data.glide

import android.content.ContentValues.TAG
import android.util.Log
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import timber.log.Timber

open class FileFetcher(private val file: File) : DataFetcher<InputStream> {

    private var data: InputStream? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        loadFromFile(callback)
    }

    protected fun loadFromFile(callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            data = FileInputStream(file)
        } catch (e: FileNotFoundException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Timber.d(e, "Failed to open file")
            }
            callback.onLoadFailed(e)
            return
        }

        callback.onDataReady(data)
    }

    override fun cleanup() {
        try {
            data?.close()
        } catch (e: IOException) {
            // Ignored.
        }
    }

    override fun cancel() {
        // Do nothing.
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}
