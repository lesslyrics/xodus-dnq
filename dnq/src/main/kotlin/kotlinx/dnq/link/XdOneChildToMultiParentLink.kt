/**
 * Copyright 2006 - 2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.dnq.link

import jetbrains.exodus.query.metadata.AssociationEndCardinality
import jetbrains.exodus.query.metadata.AssociationEndType
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdEntityType
import kotlinx.dnq.util.reattach
import kotlinx.dnq.util.reattachAndGetLink
import kotlinx.dnq.util.threadSessionOrThrow
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class XdOneChildToMultiParentLink<R : XdEntity, T : XdEntity>(
        oppositeEntityType: XdEntityType<T>,
        override val oppositeField: KProperty1<T, R?>,
        dbPropertyName: String?
) : ScalarOptionalLink<R, T>, XdLink<R, T>(
        oppositeEntityType,
        dbPropertyName,
        null,
        AssociationEndCardinality._0_1,
        AssociationEndType.ChildEnd,
        onDelete = OnDeletePolicy.CLEAR,
        onTargetDelete = OnDeletePolicy.CASCADE
) {

    override fun getValue(thisRef: R, property: KProperty<*>): T? {
        return thisRef.reattachAndGetLink(property.name)?.let { value ->
            oppositeEntityType.wrap(value)
        }
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T?) {
        val session = thisRef.threadSessionOrThrow
        val parent = value?.reattach(session)
        val child = thisRef.reattach(session)
        if (parent != null) {
            parent.setChild(oppositeField.name, property.name, child)
        } else {
            child.removeFromParent(oppositeField.name, property.name)
        }
    }

    override fun isDefined(thisRef: R, property: KProperty<*>) = getValue(thisRef, property) != null
}

