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

package org.cadixdev.lorenz.impl.model;

import org.cadixdev.bombe.analysis.InheritanceProvider;
import org.cadixdev.bombe.analysis.InheritanceType;
import org.cadixdev.bombe.type.MethodDescriptor;
import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.MethodMapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A basic implementation of {@link ClassMapping}.
 *
 * @param <M> The type of the class mapping
 * @param <P> The type of the parent
 *
 * @author Jamie Mansfield
 * @since 0.2.0
 */
public abstract class AbstractClassMappingImpl<M extends ClassMapping, P>
        extends AbstractMappingImpl<M, P>
        implements ClassMapping<M, P> {

    private volatile Map<FieldSignature, FieldMapping> fields;
    private volatile Map<String, FieldMapping> fieldsByName;
    private volatile Map<MethodSignature, MethodMapping> methods;
    private volatile Map<String, InnerClassMapping> innerClasses;
    private boolean complete;

    /**
     * Creates a new class mapping, from the given parameters.
     *
     * @param mappings The mappings set, this mapping belongs to
     * @param obfuscatedName The obfuscated name
     * @param deobfuscatedName The de-obfuscated name
     */
    protected AbstractClassMappingImpl(final MappingSet mappings, final String obfuscatedName, final String deobfuscatedName) {
        super(mappings, obfuscatedName, deobfuscatedName);
    }

    private Map<FieldSignature, FieldMapping> fieldMap() {
        Map<FieldSignature, FieldMapping> f = this.fields;
        if (f == null) {
            synchronized (this) {
                f = this.fields;
                if (f == null) this.fields = f = new ConcurrentHashMap<>();
            }
        }
        return f;
    }

    private Map<String, FieldMapping> fieldsByNameMap() {
        Map<String, FieldMapping> f = this.fieldsByName;
        if (f == null) {
            synchronized (this) {
                f = this.fieldsByName;
                if (f == null) this.fieldsByName = f = new ConcurrentHashMap<>();
            }
        }
        return f;
    }

    private Map<MethodSignature, MethodMapping> methodMap() {
        Map<MethodSignature, MethodMapping> m = this.methods;
        if (m == null) {
            synchronized (this) {
                m = this.methods;
                if (m == null) this.methods = m = new ConcurrentHashMap<>();
            }
        }
        return m;
    }

    private Map<String, InnerClassMapping> innerClassMap() {
        Map<String, InnerClassMapping> i = this.innerClasses;
        if (i == null) {
            synchronized (this) {
                i = this.innerClasses;
                if (i == null) this.innerClasses = i = new ConcurrentHashMap<>();
            }
        }
        return i;
    }

    @Override
    public Collection<FieldMapping> getFieldMappings() {
        final Map<FieldSignature, FieldMapping> f = this.fields;
        return f == null ? Collections.emptyList() : Collections.unmodifiableCollection(f.values());
    }

    @Override
    public Map<String, FieldMapping> getFieldsByName() {
        final Map<String, FieldMapping> f = this.fieldsByName;
        return f == null ? Collections.emptyMap() : Collections.unmodifiableMap(f);
    }

    @Override
    public Optional<FieldMapping> getFieldMapping(final FieldSignature signature) {
        final Map<FieldSignature, FieldMapping> f = this.fields;
        return f == null ? Optional.empty() : Optional.ofNullable(f.get(signature));
    }

    @Override
    public Optional<FieldMapping> getFieldMapping(final String obfuscatedName) {
        final Map<String, FieldMapping> f = this.fieldsByName;
        return f == null ? Optional.empty() : Optional.ofNullable(f.get(obfuscatedName));
    }

    @Override
    public Optional<FieldMapping> computeFieldMapping(final FieldSignature signature) {
        // If the field type is not provided, lookup up only the field name
        if (!signature.getType().isPresent()) {
            return this.getFieldMapping(signature.getName());
        }

        // Otherwise, look up the signature as-is, but attempt falling back to a signature without type
        // Note: We cannot use fieldsByName here, because we'd eventually return FieldMappings with the wrong type
        final Map<FieldSignature, FieldMapping> f = this.fieldMap();
        return Optional.ofNullable(f.computeIfAbsent(signature, (sig) -> {
            final FieldMapping mapping = f.get(new FieldSignature(sig.getName()));
            return mapping != null ?
                    this.getMappings().getModelFactory().createFieldMapping(mapping.getParent(), sig, mapping.getDeobfuscatedName()) : null;
        }));
    }

    @Override
    public FieldMapping createFieldMapping(final FieldSignature signature, final String deobfuscatedName) {
        return this.fieldMap().compute(signature, (sig, existingMapping) -> {
            if (existingMapping != null) return existingMapping.setDeobfuscatedName(deobfuscatedName);
            final FieldMapping mapping = this.getMappings().getModelFactory().createFieldMapping(this, sig, deobfuscatedName);
            this.fieldsByNameMap().put(sig.getName(), mapping);
            return mapping;
        });
    }

    @Override
    public boolean hasFieldMapping(final FieldSignature signature) {
        return this.getFieldMapping(signature).isPresent();
    }

    @Override
    public boolean hasFieldMapping(final String obfuscatedName) {
        final Map<String, FieldMapping> f = this.fieldsByName;
        return f != null && f.containsKey(obfuscatedName);
    }

    @Override
    public void removeFieldMapping(final FieldSignature signature) {
        final Map<FieldSignature, FieldMapping> f = this.fields;
        if (f == null) return;
        final FieldMapping mapping = f.remove(signature);
        if (mapping != null && this.fieldsByName != null) {
            this.fieldsByName.values().remove(mapping);
        }
    }

    @Override
    public void removeFieldMapping(final FieldMapping mapping) {
        if (this.fields != null) this.fields.values().remove(mapping);
        if (this.fieldsByName != null) this.fieldsByName.values().remove(mapping);
    }

    @Override
    public void removeFieldMapping(final String obfuscatedName) {
        if (this.fields != null) this.fields.keySet().removeIf(sig -> sig.getName().equals(obfuscatedName));
        if (this.fieldsByName != null) this.fieldsByName.remove(obfuscatedName);
    }

    @Override
    public Collection<MethodMapping> getMethodMappings() {
        final Map<MethodSignature, MethodMapping> m = this.methods;
        return m == null ? Collections.emptyList() : Collections.unmodifiableCollection(m.values());
    }

    @Override
    public Optional<MethodMapping> getMethodMapping(final MethodSignature signature) {
        final Map<MethodSignature, MethodMapping> m = this.methods;
        return m == null ? Optional.empty() : Optional.ofNullable(m.get(signature));
    }

    @Override
    public MethodMapping createMethodMapping(final MethodSignature signature, final String deobfuscatedName) {
        return this.methodMap().compute(signature, (desc, existingMapping) -> {
            if (existingMapping != null) return existingMapping.setDeobfuscatedName(deobfuscatedName);
            return this.getMappings().getModelFactory().createMethodMapping(this, signature, deobfuscatedName);
        });
    }

    @Override
    public boolean hasMethodMapping(final MethodSignature signature) {
        final Map<MethodSignature, MethodMapping> m = this.methods;
        return m != null && m.containsKey(signature);
    }

    @Override
    public void removeMethodMapping(final MethodSignature signature) {
        if (this.methods != null) this.methods.remove(signature);
    }

    @Override
    public void removeMethodMapping(final MethodMapping mapping) {
        if (this.methods != null) this.methods.values().remove(mapping);
    }

    @Override
    public Collection<InnerClassMapping> getInnerClassMappings() {
        final Map<String, InnerClassMapping> i = this.innerClasses;
        return i == null ? Collections.emptyList() : Collections.unmodifiableCollection(i.values());
    }

    @Override
    public Optional<InnerClassMapping> getInnerClassMapping(final String obfuscatedName) {
        final Map<String, InnerClassMapping> i = this.innerClasses;
        return i == null ? Optional.empty() : Optional.ofNullable(i.get(obfuscatedName));
    }

    @Override
    public InnerClassMapping createInnerClassMapping(final String obfuscatedName, final String deobfuscatedName) {
        return this.innerClassMap().compute(obfuscatedName, (name, existingMapping) -> {
            if (existingMapping != null) return existingMapping.setDeobfuscatedName(deobfuscatedName);
            return this.getMappings().getModelFactory().createInnerClassMapping(this, obfuscatedName, deobfuscatedName);
        });
    }

    @Override
    public boolean hasInnerClassMapping(final String obfuscatedName) {
        final Map<String, InnerClassMapping> i = this.innerClasses;
        return i != null && i.containsKey(obfuscatedName);
    }

    @Override
    public void removeInnerClassMapping(String obfuscatedName) {
        if (this.innerClasses != null) this.innerClasses.remove(obfuscatedName);
    }

    @Override
    public void removeInnerClassMapping(final ClassMapping<?, ?> mapping) {
        if (this.innerClasses != null) this.innerClasses.values().remove(mapping);
    }

    @Override
    protected StringJoiner buildToString() {
        return super.buildToString()
                .add("fields=" + this.getFieldMappings())
                .add("methods=" + this.getMethodMappings())
                .add("innerClasses=" + this.getInnerClassMappings());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof ClassMapping)) return false;

        final ClassMapping that = (ClassMapping) obj;
        return Objects.equals(this.getFieldMappings(), that.getFieldMappings()) &&
                Objects.equals(this.getMethodMappings(), that.getMethodMappings()) &&
                Objects.equals(this.getInnerClassMappings(), that.getInnerClassMappings());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getFieldMappings(), this.getMethodMappings(), this.getInnerClassMappings());
    }

    @Override
    public boolean isComplete() {
        return this.complete;
    }

    @Override
    public void complete(final InheritanceProvider provider, final InheritanceProvider.ClassInfo info) {
        if (this.complete) {
            return;
        }

        final Map<String, Set<MethodSignature>> nameToMethods = new HashMap<>();
        for (final Map.Entry<MethodSignature, InheritanceType> method : info.getMethods().entrySet()) {
            final Set<MethodSignature> methods = nameToMethods.computeIfAbsent(method.getKey().getName(), name -> new HashSet<>());
            methods.add(method.getKey());
        }

        for (final InheritanceProvider.ClassInfo parent : info.provideParents(provider)) {
            final ClassMapping<?, ?> parentMappings = this.getMappings().getOrCreateClassMapping(parent.getName());
            parentMappings.complete(provider, parent);

            for (final FieldMapping mapping : parentMappings.getFieldMappings()) {
                // If the class has its own field that satisfies the parent's signature,
                // then we shouldn't inherit the mapping
                if (this.computeFieldMapping(mapping.getSignature()).isPresent()) {
                    continue;
                }

                if (parent.canInherit(info, mapping.getSignature())) {
                    this.fieldMap().putIfAbsent(mapping.getSignature(), mapping);
                }
            }

            for (final MethodMapping mapping : parentMappings.getMethodMappings()) {
                if (parent.canInherit(info, mapping.getSignature())) {
                    this.methodMap().putIfAbsent(mapping.getSignature(), mapping);
                }

                // Check if there are any methods here that override the return type of a parent
                // method.
                if (nameToMethods.containsKey(mapping.getObfuscatedName())) {
                    for (final MethodSignature methodSignature : nameToMethods.get(mapping.getObfuscatedName())) {
                        final MethodDescriptor methodDescriptor = methodSignature.getDescriptor();

                        final MethodSignature mappingSignature = mapping.getSignature();
                        final MethodDescriptor mappingDescriptor = mappingSignature.getDescriptor();

                        // The method MUST have the same parameters
                        // TODO: handle generic params
                        if (!Objects.equals(methodDescriptor.getParamTypes(), mappingDescriptor.getParamTypes())) continue;

                        if (mappingDescriptor.getReturnType().isAssignableFrom(methodDescriptor.getReturnType(), provider)) {
                            this.methodMap().putIfAbsent(methodSignature, mapping);
                        }
                    }
                }
            }
        }

        this.complete = true;
    }

}
