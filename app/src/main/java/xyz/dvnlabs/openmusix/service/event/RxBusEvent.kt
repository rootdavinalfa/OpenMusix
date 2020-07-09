/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.service.event

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject

/*This object used as event bus on OpenMusix.
We not used EventBus because it may have boiler plate code and
make the code look like spaghetti.
Thus, we depend heavily RxJava / RxKotlin in this case.*/
object RxBusEvent {
    private val publisher = PublishSubject.create<Any>()
    fun publish(event: Any) {
        publisher.onNext(event)
    }

    /**Listen should return an Observable and not the publisher
     * Using ofType we filter only events that match that class type
     *
     * [eventType] Fill this with Event Class
     *
     **/
    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}