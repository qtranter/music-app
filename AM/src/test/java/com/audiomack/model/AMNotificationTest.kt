package com.audiomack.model

import com.audiomack.TestApplication
import com.nhaarman.mockitokotlin2.mock
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class
)
class AMNotificationTest {

    @Test
    fun `benchmark notification parsing`() {
        val benchmarkType = "plays"
        val benchmarkMilestone = 1000L

        val json = JSONObject().apply {
            put("verb", "benchmark")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("extra_context", JSONObject().apply {
                put("count", benchmarkMilestone)
                put("type", benchmarkType)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.Benchmark, notification!!.verb)
        assertNotNull(notification.createdAt)
        assert(notification.isSeen)
        assert(notification.type is AMNotification.NotificationType.Benchmark)
        assertEquals(benchmarkType, (notification.type as AMNotification.NotificationType.Benchmark).benchmark.type.stringCode)
        assertEquals(benchmarkMilestone, (notification.type as AMNotification.NotificationType.Benchmark).benchmark.milestone)
        assertNotNull(notification.`object`)
        assertTrue(notification.`object` is AMResultItem)
    }

    @Test
    fun `favorite notification parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "favorite")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.Favorite, notification!!.verb)
        assertNotNull(notification.createdAt)
        assert(notification.isSeen)
        assertEquals(actorName, notification.author?.name)
        assertEquals(actorSlug, notification.author?.slug)
        assertEquals(actorImage, notification.author?.image)
        assertNotNull(notification.`object`)
        assertTrue(notification.`object` is AMResultItem)
    }

    @Test
    fun `follow notification parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "follow")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", false)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.Follow, notification!!.verb)
        assertNotNull(notification.createdAt)
        assertEquals(false, notification.isSeen)
        assertEquals(actorName, notification.author?.name)
        assertEquals(actorSlug, notification.author?.slug)
        assertEquals(actorImage, notification.author?.image)
        assertNotNull(notification.`object`)
        assertTrue(notification.`object` is AMArtist)
    }

    @Test
    fun `follow malformed notification (missing actor) parsing`() {
        val json = JSONObject().apply {
            put("verb", "follow")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", false)
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `repost notification parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "reup")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.Repost, notification!!.verb)
        assertNotNull(notification.createdAt)
        assert(notification.isSeen)
        assertEquals(actorName, notification.author?.name)
        assertEquals(actorSlug, notification.author?.slug)
        assertEquals(actorImage, notification.author?.image)
        assertNotNull(notification.`object`)
        assertTrue(notification.`object` is AMResultItem)
    }

    @Test
    fun `repost malformed notification (missing object) parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "reup")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `playlisted notification parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "playlisted")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.Playlist, notification!!.verb)
        assertNotNull(notification.createdAt)
        assert(notification.isSeen)
        assertEquals(actorName, notification.author?.name)
        assertEquals(actorSlug, notification.author?.slug)
        assertEquals(actorImage, notification.author?.image)
        assertNotNull(notification.`object`)
        assertTrue(notification.`object` is AMResultItem)
    }

    @Test
    fun `playlist favorited notification parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "playlistfavorite")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.FavoritePlaylist, notification!!.verb)
        assertNotNull(notification.createdAt)
        assert(notification.isSeen)
        assertEquals(actorName, notification.author?.name)
        assertEquals(actorSlug, notification.author?.slug)
        assertEquals(actorImage, notification.author?.image)
        assertNotNull(notification.`object`)
        assertTrue(notification.`object` is AMResultItem)
    }

    @Test
    fun `playlist favorited malformed notification (missing object) parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "playlistfavorite")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `playlist updated notification parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "playlist_updated")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
            put("target", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.PlaylistUpdated, notification!!.verb)
        assertNotNull(notification.createdAt)
        assert(notification.isSeen)
        assertEquals(actorName, notification.author?.name)
        assertEquals(actorSlug, notification.author?.slug)
        assertEquals(actorImage, notification.author?.image)
        assertNotNull(notification.`object`)
        assertTrue(notification.`object` is AMResultItem)
        assertNotNull(notification.target)
    }

    @Test
    fun `playlist updated malformed notification (missing target) parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "playlist_updated")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `comment notification`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"
        val comment = "Nice song"

        val json = JSONObject().apply {
            put("verb", "comment")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
            put("extra_context", JSONObject().apply {
                put("comment", comment)
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.Comment, notification!!.verb)
        assert(notification.type is AMNotification.NotificationType.Comment)
        assertEquals(comment, (notification.type as AMNotification.NotificationType.Comment).comment)
        assertNull(null, (notification.type as AMNotification.NotificationType.Comment).commentReply)
    }

    @Test
    fun `comment reply notification`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"
        val comment = "Nice song"
        val reply = "True dat"

        val json = JSONObject().apply {
            put("verb", "comment")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
            put("extra_context", JSONObject().apply {
                put("comment", reply)
                put("parent", comment)
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNotNull(notification)
        assertEquals(AMNotification.AMNotificationVerb.Comment, notification!!.verb)
        assert(notification.type is AMNotification.NotificationType.Comment)
        assertEquals(reply, (notification.type as AMNotification.NotificationType.Comment).commentReply)
        assertEquals(comment, (notification.type as AMNotification.NotificationType.Comment).comment)
    }

    @Test
    fun `comment malformed (invalid extra context) notification`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "comment")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `comment malformed (no comment) notification`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "comment")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
            put("extra_context", JSONObject())
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `play benchmark notification`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"
        val count = 1000L

        val json = JSONObject().apply {
            put("verb", "benchmark")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
            put("extra_context", JSONObject().apply {
                put("count", count)
                put("type", "plays")
            })
        }
        val notification = AMNotification.fromJSON(json)
        assert(notification!!.type is AMNotification.NotificationType.Benchmark)
        assertEquals(BenchmarkType.PLAY, (notification.type as AMNotification.NotificationType.Benchmark).benchmark.type)
        assertEquals(count, (notification.type as AMNotification.NotificationType.Benchmark).benchmark.milestone)
    }

    @Test
    fun `unsupported benchmark notification`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("verb", "benchmark")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
            put("extra_context", JSONObject().apply {
                put("type", "favorites")
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `comment upvote notification`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"
        val count = 1000L
        val thread = "1111"
        val uuid = "1234"

        val json = JSONObject().apply {
            put("verb", "benchmark")
            put("created_at", "2019-03-15T01:58:50.000000")
            put("seen", true)
            put("actor", JSONObject().apply {
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image", actorImage)
            })
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
            put("extra_context", JSONObject().apply {
                put("count", count)
                put("type", "comment-upvotes")
                put("thread", thread)
                put("uuid", uuid)
            })
        }
        val notification = AMNotification.fromJSON(json)
        assert(notification!!.type is AMNotification.NotificationType.UpvoteComment)
        assertEquals(count, (notification.type as AMNotification.NotificationType.UpvoteComment).data.count)
        assertEquals(thread, (notification.type as AMNotification.NotificationType.UpvoteComment).data.threadId)
        assertEquals(uuid, (notification.type as AMNotification.NotificationType.UpvoteComment).data.uuid)
    }

    @Test
    fun `playlists bundle update notification`() {
        val json = JSONObject().apply {
            put("verb", "playlist_updated_bundle")
            put("extra_context", JSONObject().apply {
                put("playlists", JSONArray().apply {
                    put(JSONObject())
                })
                put("image_base_added_songs", JSONArray().apply {
                    put("https://link")
                    put("https://linkk")
                    put("https://linkkk")
                })
            })
        }
        val notification = AMNotification.fromJSON(json)
        assert(notification!!.type is AMNotification.NotificationType.PlaylistUpdatedBundle)
        assert((notification.type as AMNotification.NotificationType.PlaylistUpdatedBundle).playlists.size == 1)
        assert((notification.type as AMNotification.NotificationType.PlaylistUpdatedBundle).songsImages.size == 3)
    }

    @Test
    fun `playlists bundle update notification, invalid because there are no playlists`() {
        val json = JSONObject().apply {
            put("verb", "playlist_updated_bundle")
            put("extra_context", JSONObject().apply {
                put("image_base_added_songs", JSONArray().apply {
                    put("https://link")
                    put("https://linkk")
                    put("https://linkkk")
                })
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `playlists bundle update notification, invalid because there are no songs`() {
        val json = JSONObject().apply {
            put("verb", "playlist_updated_bundle")
            put("extra_context", JSONObject().apply {
                put("playlists", JSONArray().apply {
                    mock<AMResultItem>()
                })
                put("image_base_added_songs", JSONArray())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }

    @Test
    fun `unknown notification with missing data parsing`() {
        val json = JSONObject().apply {
            put("verb", "debugged")
            put("object", JSONObject().apply {
                put("uploader", JSONObject())
            })
        }
        val notification = AMNotification.fromJSON(json)
        assertNull(notification)
    }
}
