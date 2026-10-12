package com.wayfarer.data

/**
 * Photos are copied into `<app storage>/photos`. The absolute location of app storage can change (iOS gives the
 * app a new container path after an update, restore or reinstall), so the database keeps only the file name and
 * the full path is rebuilt from the CURRENT photos folder every time an entry is loaded.
 *
 * Rows written before this existed hold a full path; taking the last segment makes them resolve correctly too.
 */
class PhotoLocator(private val photosDir: String) {
    /** What goes into the database. */
    fun toStored(path: String): String = fileName(path)

    /** What the UI loads (a real path under the current photos folder). */
    fun resolve(stored: String): String = "${photosDir.trimEnd('/')}/${fileName(stored)}"

    private fun fileName(path: String) = path.substringAfterLast('/')
}
