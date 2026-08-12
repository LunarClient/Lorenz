/*
 * This file is part of Lorenz, licensed under the MIT License (MIT).
 *
 * Copyright (c) Jamie Mansfield <https://www.jamierocks.uk/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.cadixdev.lorenz.util;

import org.cadixdev.bombe.type.FieldType;
import org.cadixdev.bombe.type.MethodDescriptor;
import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

public final class Signatures {

    private static final boolean ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("lorenz.signatureCache"));

    private static final boolean VERIFY = Boolean.getBoolean("lorenz.verifySignatureCache");

    private static final int MAX_ENTRIES = Integer.getInteger("lorenz.signatureCacheMax", 1 << 18);

    private static final Map<String, MethodDescriptor> DESCRIPTORS = new ConcurrentHashMap<>();
    private static final Map<String, FieldType> FIELD_TYPES = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, MethodSignature>> METHODS = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, FieldSignature>> FIELDS = new ConcurrentHashMap<>();

    private Signatures() {
    }

    public static MethodDescriptor descriptor(final String descriptor) {
        if (!ENABLED) return MethodDescriptor.of(descriptor);

        final MethodDescriptor cached = DESCRIPTORS.get(descriptor);
        if (cached != null) {
            return VERIFY ? verify(cached, MethodDescriptor.of(descriptor), descriptor) : cached;
        }

        final MethodDescriptor parsed = MethodDescriptor.of(descriptor);
        if (DESCRIPTORS.size() < MAX_ENTRIES) DESCRIPTORS.putIfAbsent(descriptor, parsed);
        return parsed;
    }

    public static FieldType fieldType(final String type) {
        if (!ENABLED) return FieldType.of(type);

        final FieldType cached = FIELD_TYPES.get(type);
        if (cached != null) {
            return VERIFY ? verify(cached, FieldType.of(type), type) : cached;
        }

        final FieldType parsed = FieldType.of(type);
        if (FIELD_TYPES.size() < MAX_ENTRIES) FIELD_TYPES.putIfAbsent(type, parsed);
        return parsed;
    }

    public static MethodSignature method(final String name, final String descriptor) {
        if (!ENABLED) return MethodSignature.of(name, descriptor);

        final Map<String, MethodSignature> byName = METHODS.get(descriptor);
        if (byName != null) {
            final MethodSignature cached = byName.get(name);
            if (cached != null) {
                return VERIFY ? verify(cached, MethodSignature.of(name, descriptor), name + descriptor) : cached;
            }
        }

        final MethodSignature parsed = new HashedMethodSignature(name, descriptor(descriptor));
        final Map<String, MethodSignature> table = byName != null ? byName
                : METHODS.size() < MAX_ENTRIES ? METHODS.computeIfAbsent(descriptor, d -> new ConcurrentHashMap<>())
                : null;
        if (table != null && table.size() < MAX_ENTRIES) table.putIfAbsent(name, parsed);
        return parsed;
    }

    public static FieldSignature field(final String name, final String type) {
        if (!ENABLED) return FieldSignature.of(name, type);

        final Map<String, FieldSignature> byName = FIELDS.get(type);
        if (byName != null) {
            final FieldSignature cached = byName.get(name);
            if (cached != null) {
                return VERIFY ? verify(cached, FieldSignature.of(name, type), name + type) : cached;
            }
        }

        final FieldSignature parsed = new HashedFieldSignature(name, fieldType(type));
        final Map<String, FieldSignature> table = byName != null ? byName
                : FIELDS.size() < MAX_ENTRIES ? FIELDS.computeIfAbsent(type, t -> new ConcurrentHashMap<>())
                : null;
        if (table != null && table.size() < MAX_ENTRIES) table.putIfAbsent(name, parsed);
        return parsed;
    }

    private static <T> T verify(final T cached, final T fresh, final String input) {
        if (!Objects.equals(cached, fresh)
                || !Objects.equals(fresh, cached)
                || cached.hashCode() != fresh.hashCode()
                || !String.valueOf(cached).equals(String.valueOf(fresh))) {
            throw new IllegalStateException("Signature cache disagreement for \"" + input
                    + "\": cached=" + cached + " fresh=" + fresh);
        }
        return cached;
    }

    private static final class HashedMethodSignature extends MethodSignature {

        private final int hash;

        HashedMethodSignature(final String name, final MethodDescriptor descriptor) {
            super(name, descriptor);
            this.hash = Objects.hash(name, descriptor);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        protected StringJoiner buildToString() {
            return new StringJoiner(", ", MethodSignature.class.getSimpleName() + "{", "}")
                    .add("name=" + this.getName())
                    .add("descriptor=" + this.getDescriptor());
        }

    }

    private static final class HashedFieldSignature extends FieldSignature {

        private final int hash;

        HashedFieldSignature(final String name, final FieldType type) {
            super(name, type);
            this.hash = Objects.hash(name, type);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        protected StringJoiner buildToString() {
            return new StringJoiner(", ", FieldSignature.class.getSimpleName() + "{", "}")
                    .add("name=" + this.getName())
                    .add("type=" + this.getType().orElse(null));
        }

    }

}
