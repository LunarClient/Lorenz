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

package org.cadixdev.lorenz.merge;

import org.cadixdev.lorenz.MappingSet;

/**
 * <p>
 * This enum represents a configuration value for determining how strictly field mappings will be merged via
 * {@link MappingSetMerger}. Use it with {@link MergeConfig} and {@link MappingSetMerger#create(MappingSet, MappingSet, MergeConfig)}.
 *
 * @author Kyle Wood
 * @since 0.5.4
 */
public enum FieldMergeStrategy {
    /**
     * Only match exactly. This either means:
     * <ul>
     *     <li>For continuation merges, the full deobfuscated output signature of the left mapping must match the
     *         full obfuscated input signature of the right mapping, including field type.</li>
     *     <li>For duplicate merges, the full obfuscated input signature of the left mapping must match the full
     *         obfuscated input signature of the right mapping, including field type.</li>
     * </ul>
     * For more information regarding continuation and duplicate merges, see the <b>Continuations and Duplicates</b>
     * section of {@link MappingSetMergerHandler}.
     *
     * @since 0.5.4
     */
    STRICT,
    /**
     * Match the same as {@link #STRICT}, but also match more broadly:
     * <ul>
     *     <li>For continuation merges, only the deobfuscated output name of the left mapping must match the obfuscated
     *         input name of the right mapping, ignoring the type.</li>
     *     <li>For duplicate merges, only the obfuscated input name of the left mapping needs to match the obfuscated
     *         input name of the right mapping, ignoring the type.</li>
     * </ul>
     * For more information regarding continuation and duplicate merges, see the <b>Continuations and Duplicates</b>
     * section of {@link MappingSetMergerHandler}.
     *
     * @since 0.5.4
     */
    LOOSE
}
