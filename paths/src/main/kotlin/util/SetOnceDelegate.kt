package util

import kotlin.reflect.KProperty

class SetOnceDelegate<T> {
    private var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException("${property.name} not initialized")
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        if (value != null) {
            value = newValue
        }
    }
}