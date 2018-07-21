package com.kanedias.dybr.fair.misc

import moe.banana.jsonapi2.ResourceIdentifier

infix fun ResourceIdentifier.idMatches(other: ResourceIdentifier): Boolean {
    return this.id == other.id && this.type == other.type
}