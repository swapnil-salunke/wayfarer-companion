package com.wayfarer

import com.wayfarer.data.PhotoLocator
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoLocatorTest {
    private val locator = PhotoLocator("/new/container/Documents/photos")

    @Test fun storesOnlyTheFileName() =
        assertEquals("a.jpg", locator.toStored("/old/container/Documents/photos/a.jpg"))

    @Test fun resolvesAgainstTheCurrentPhotosFolder() =
        assertEquals("/new/container/Documents/photos/a.jpg", locator.resolve("a.jpg"))

    @Test fun legacyAbsolutePathFromAnOldContainerStillResolves() =
        assertEquals("/new/container/Documents/photos/a.jpg", locator.resolve("/old/container/Documents/photos/a.jpg"))

    @Test fun trailingSlashInFolderIsIgnored() =
        assertEquals("/p/a.jpg", PhotoLocator("/p/").resolve("a.jpg"))

    @Test fun roundTrip() {
        val stored = locator.toStored("/anywhere/photos/x.jpg")
        assertEquals("/new/container/Documents/photos/x.jpg", locator.resolve(stored))
    }
}
