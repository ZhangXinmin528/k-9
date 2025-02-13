package com.mail.ann

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Robolectric test that does not create an instance of our [Application] class [Ann].
 *
 * See also [K9RobolectricTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyApplication::class)
abstract class RobolectricTest

class EmptyApplication : Application()
