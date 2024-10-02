/* 
  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
  
  Licensed under the Apache License, Version 2.0 (the "License").
  You may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package com.amazon.solutions.druid.cloudwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class MemoryBoundLinkedBlockingQueue<T> {

    private final long memoryBound;

    private final AtomicLong currentMemory;

    private final LinkedBlockingQueue<ObjectContainer<T>> queue;

    public MemoryBoundLinkedBlockingQueue(final long memoryBound) {
        this(memoryBound, new LinkedBlockingQueue<>(), new AtomicLong(0L));
    }

    public MemoryBoundLinkedBlockingQueue(final long memoryBound, final LinkedBlockingQueue<ObjectContainer<T>> queue,
                                          final AtomicLong currentMemory) {
        this.memoryBound = memoryBound;
        this.currentMemory = currentMemory;
        this.queue = queue;
    }

    public int size() {
        return this.queue.size();
    }

    public AtomicLong getCurrentMemory() {
        return this.currentMemory;
    }

    public synchronized boolean offer(ObjectContainer<T> item) {

        final long itemLength = item.getSize();

        if (currentMemory.addAndGet(itemLength) <= memoryBound && queue.offer(item)) {
            return true;
        }
        currentMemory.addAndGet(-itemLength);
        return false;
    }

    public synchronized ObjectContainer<T> take() throws InterruptedException {
        final ObjectContainer<T> ret = queue.take();
        currentMemory.addAndGet(-ret.getSize());
        return ret;
    }

    public synchronized List<ObjectContainer<T>> take(final int elementSize) throws InterruptedException {

        final List<ObjectContainer<T>> elements = new ArrayList<>();
        final int sizeToReturn = (elementSize > queue.size()) ? queue.size() : elementSize;

        for (int i = 0; i < sizeToReturn; i++) {
            elements.add(take());
        }
        return elements;
    }

    public static class ObjectContainer<T> {
        private T data;
        private long size;

        ObjectContainer(T data, long size) {
            this.data = data;
            this.size = size;
        }

        public T getData()
        {
            return data;
        }

        public long getSize()
        {
            return size;
        }
    }
}