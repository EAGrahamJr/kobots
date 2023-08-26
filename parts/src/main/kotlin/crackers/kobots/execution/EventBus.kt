/*
 * Copyright 2022-2023 by E. A. Graham, Jr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package crackers.kobots.execution

import org.slf4j.LoggerFactory
import org.tinylog.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Flow
import java.util.concurrent.SubmissionPublisher

interface KobotsMessage
interface KobotsAction : KobotsMessage {
    val interruptable: Boolean
}

interface KobotsEvent : KobotsMessage

private val eventBusMap = ConcurrentHashMap<String, SubmissionPublisher<*>>()

/**
 * Receives an item from a topic. See [joinTopic]
 */
fun interface KobotsSubscriber<T : KobotsMessage> {
    fun receive(msg: T)
}

/**
 * Wraps the subscriber in all the extra stuff necessary. Requests [batchSize] items.
 */
private class KobotsSubscriberDecorator<T : KobotsMessage>(val listener: KobotsSubscriber<T>, val batchSize: Long) :
    Flow.Subscriber<T> {
    private lateinit var mySub: Flow.Subscription
    override fun onSubscribe(subscription: Flow.Subscription) {
        mySub = subscription
        mySub.request(batchSize)
    }

    override fun onNext(item: T) {
        try {
            listener.receive(item)
        } catch (t: Throwable) {
            Logger.error(t, "Error in bus")
        }
        mySub.request(batchSize)
    }

    override fun onError(throwable: Throwable?) {
        LoggerFactory.getLogger("EventSubscriber").error("Error in bus", throwable)
    }

    override fun onComplete() {
//        TODO("Not yet implemented")
    }
}

/**
 * Get items (default 1 at a time) asynchronously.
 *
 * TODO type safety is a problem here?
 */
@Suppress("UNCHECKED_CAST")
fun <T : KobotsMessage> joinTopic(topic: String, listener: KobotsSubscriber<T>, batchSize: Long = 1) {
    val publisher = eventBusMap.computeIfAbsent(topic) { SubmissionPublisher<T>() } as SubmissionPublisher<T>
    publisher.subscribe(KobotsSubscriberDecorator(listener, batchSize))
}

/**
 * Stop getting items.
 */
@Suppress("UNCHECKED_CAST")
fun <T : KobotsMessage> leaveTopic(topic: String, listener: KobotsSubscriber<T>) {
    val publisher = eventBusMap.computeIfAbsent(topic) { SubmissionPublisher<T>() } as SubmissionPublisher<T>
    publisher.subscribers.removeIf { (it as KobotsSubscriberDecorator<T>).listener == listener }
}

/**
 * Publish one or more [items] to a [topic].
 */
fun <T : KobotsMessage> publishToTopic(topic: String, vararg items: T) {
    publishToTopic(topic, items.toList())
}

/**
 * Publish a collection of [items] to a [topic]
 *
 * TODO type safety is a problem here?
 */
@Suppress("UNCHECKED_CAST")
fun <T : KobotsMessage> publishToTopic(topic: String, items: Collection<T>) {
    val publisher = eventBusMap.computeIfAbsent(topic) { SubmissionPublisher<T>() } as SubmissionPublisher<T>
    items.forEach { item -> publisher.submit(item) }
}

// specific messages ==================================================================================================
class EmergencyStop() : KobotsAction {
    override val interruptable: Boolean = false
}

val allStop = EmergencyStop()
