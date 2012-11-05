#Signature file v4.1
#Version 2.0-SNAPSHOT

CLSS public abstract interface java.io.Serializable

CLSS public abstract interface java.lang.Comparable<%0 extends java.lang.Object>
meth public abstract int compareTo({java.lang.Comparable%0})

CLSS public abstract java.lang.Enum<%0 extends java.lang.Enum<{java.lang.Enum%0}>>
cons protected <init>(java.lang.String,int)
intf java.io.Serializable
intf java.lang.Comparable<{java.lang.Enum%0}>
meth protected final java.lang.Object clone() throws java.lang.CloneNotSupportedException
meth protected final void finalize()
meth public final boolean equals(java.lang.Object)
meth public final int compareTo({java.lang.Enum%0})
meth public final int hashCode()
meth public final int ordinal()
meth public final java.lang.Class<{java.lang.Enum%0}> getDeclaringClass()
meth public final java.lang.String name()
meth public java.lang.String toString()
meth public static <%0 extends java.lang.Enum<{%%0}>> {%%0} valueOf(java.lang.Class<{%%0}>,java.lang.String)
supr java.lang.Object
hfds name,ordinal

CLSS public java.lang.Exception
cons protected <init>(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.Throwable
hfds serialVersionUID

CLSS public abstract interface java.lang.Iterable<%0 extends java.lang.Object>
meth public abstract java.util.Iterator<{java.lang.Iterable%0}> iterator()

CLSS public java.lang.Object
cons public <init>()
meth protected java.lang.Object clone() throws java.lang.CloneNotSupportedException
meth protected void finalize() throws java.lang.Throwable
meth public boolean equals(java.lang.Object)
meth public final java.lang.Class<?> getClass()
meth public final void notify()
meth public final void notifyAll()
meth public final void wait() throws java.lang.InterruptedException
meth public final void wait(long) throws java.lang.InterruptedException
meth public final void wait(long,int) throws java.lang.InterruptedException
meth public int hashCode()
meth public java.lang.String toString()

CLSS public java.lang.RuntimeException
cons protected <init>(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.Exception
hfds serialVersionUID

CLSS public java.lang.Throwable
cons protected <init>(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
intf java.io.Serializable
meth public final java.lang.Throwable[] getSuppressed()
meth public final void addSuppressed(java.lang.Throwable)
meth public java.lang.StackTraceElement[] getStackTrace()
meth public java.lang.String getLocalizedMessage()
meth public java.lang.String getMessage()
meth public java.lang.String toString()
meth public java.lang.Throwable fillInStackTrace()
meth public java.lang.Throwable getCause()
meth public java.lang.Throwable initCause(java.lang.Throwable)
meth public void printStackTrace()
meth public void printStackTrace(java.io.PrintStream)
meth public void printStackTrace(java.io.PrintWriter)
meth public void setStackTrace(java.lang.StackTraceElement[])
supr java.lang.Object
hfds CAUSE_CAPTION,EMPTY_THROWABLE_ARRAY,NULL_CAUSE_MESSAGE,SELF_SUPPRESSION_MESSAGE,SUPPRESSED_CAPTION,SUPPRESSED_SENTINEL,UNASSIGNED_STACK,backtrace,cause,detailMessage,serialVersionUID,stackTrace,suppressedExceptions
hcls PrintStreamOrWriter,SentinelHolder,WrappedPrintStream,WrappedPrintWriter

CLSS public abstract interface java.lang.annotation.Annotation
meth public abstract boolean equals(java.lang.Object)
meth public abstract int hashCode()
meth public abstract java.lang.Class<? extends java.lang.annotation.Annotation> annotationType()
meth public abstract java.lang.String toString()

CLSS public abstract interface !annotation java.lang.annotation.Documented
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation

CLSS public abstract interface !annotation java.lang.annotation.Retention
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation
meth public abstract java.lang.annotation.RetentionPolicy value()

CLSS public abstract interface !annotation java.lang.annotation.Target
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation
meth public abstract java.lang.annotation.ElementType[] value()

CLSS public abstract interface java.util.Collection<%0 extends java.lang.Object>
intf java.lang.Iterable<{java.util.Collection%0}>
meth public abstract <%0 extends java.lang.Object> {%%0}[] toArray({%%0}[])
meth public abstract boolean add({java.util.Collection%0})
meth public abstract boolean addAll(java.util.Collection<? extends {java.util.Collection%0}>)
meth public abstract boolean contains(java.lang.Object)
meth public abstract boolean containsAll(java.util.Collection<?>)
meth public abstract boolean equals(java.lang.Object)
meth public abstract boolean isEmpty()
meth public abstract boolean remove(java.lang.Object)
meth public abstract boolean removeAll(java.util.Collection<?>)
meth public abstract boolean retainAll(java.util.Collection<?>)
meth public abstract int hashCode()
meth public abstract int size()
meth public abstract java.lang.Object[] toArray()
meth public abstract java.util.Iterator<{java.util.Collection%0}> iterator()
meth public abstract void clear()

CLSS public abstract interface java.util.List<%0 extends java.lang.Object>
intf java.util.Collection<{java.util.List%0}>
meth public abstract <%0 extends java.lang.Object> {%%0}[] toArray({%%0}[])
meth public abstract boolean add({java.util.List%0})
meth public abstract boolean addAll(int,java.util.Collection<? extends {java.util.List%0}>)
meth public abstract boolean addAll(java.util.Collection<? extends {java.util.List%0}>)
meth public abstract boolean contains(java.lang.Object)
meth public abstract boolean containsAll(java.util.Collection<?>)
meth public abstract boolean equals(java.lang.Object)
meth public abstract boolean isEmpty()
meth public abstract boolean remove(java.lang.Object)
meth public abstract boolean removeAll(java.util.Collection<?>)
meth public abstract boolean retainAll(java.util.Collection<?>)
meth public abstract int hashCode()
meth public abstract int indexOf(java.lang.Object)
meth public abstract int lastIndexOf(java.lang.Object)
meth public abstract int size()
meth public abstract java.lang.Object[] toArray()
meth public abstract java.util.Iterator<{java.util.List%0}> iterator()
meth public abstract java.util.List<{java.util.List%0}> subList(int,int)
meth public abstract java.util.ListIterator<{java.util.List%0}> listIterator()
meth public abstract java.util.ListIterator<{java.util.List%0}> listIterator(int)
meth public abstract void add(int,{java.util.List%0})
meth public abstract void clear()
meth public abstract {java.util.List%0} get(int)
meth public abstract {java.util.List%0} remove(int)
meth public abstract {java.util.List%0} set(int,{java.util.List%0})

CLSS public abstract interface java.util.Map<%0 extends java.lang.Object, %1 extends java.lang.Object>
innr public abstract interface static Entry
meth public abstract boolean containsKey(java.lang.Object)
meth public abstract boolean containsValue(java.lang.Object)
meth public abstract boolean equals(java.lang.Object)
meth public abstract boolean isEmpty()
meth public abstract int hashCode()
meth public abstract int size()
meth public abstract java.util.Collection<{java.util.Map%1}> values()
meth public abstract java.util.Set<java.util.Map$Entry<{java.util.Map%0},{java.util.Map%1}>> entrySet()
meth public abstract java.util.Set<{java.util.Map%0}> keySet()
meth public abstract void clear()
meth public abstract void putAll(java.util.Map<? extends {java.util.Map%0},? extends {java.util.Map%1}>)
meth public abstract {java.util.Map%1} get(java.lang.Object)
meth public abstract {java.util.Map%1} put({java.util.Map%0},{java.util.Map%1})
meth public abstract {java.util.Map%1} remove(java.lang.Object)

CLSS public abstract interface java.util.Queue<%0 extends java.lang.Object>
intf java.util.Collection<{java.util.Queue%0}>
meth public abstract boolean add({java.util.Queue%0})
meth public abstract boolean offer({java.util.Queue%0})
meth public abstract {java.util.Queue%0} element()
meth public abstract {java.util.Queue%0} peek()
meth public abstract {java.util.Queue%0} poll()
meth public abstract {java.util.Queue%0} remove()

CLSS public abstract interface java.util.Set<%0 extends java.lang.Object>
intf java.util.Collection<{java.util.Set%0}>
meth public abstract <%0 extends java.lang.Object> {%%0}[] toArray({%%0}[])
meth public abstract boolean add({java.util.Set%0})
meth public abstract boolean addAll(java.util.Collection<? extends {java.util.Set%0}>)
meth public abstract boolean contains(java.lang.Object)
meth public abstract boolean containsAll(java.util.Collection<?>)
meth public abstract boolean equals(java.lang.Object)
meth public abstract boolean isEmpty()
meth public abstract boolean remove(java.lang.Object)
meth public abstract boolean removeAll(java.util.Collection<?>)
meth public abstract boolean retainAll(java.util.Collection<?>)
meth public abstract int hashCode()
meth public abstract int size()
meth public abstract java.lang.Object[] toArray()
meth public abstract java.util.Iterator<{java.util.Set%0}> iterator()
meth public abstract void clear()

CLSS public abstract interface java.util.SortedMap<%0 extends java.lang.Object, %1 extends java.lang.Object>
intf java.util.Map<{java.util.SortedMap%0},{java.util.SortedMap%1}>
meth public abstract java.util.Collection<{java.util.SortedMap%1}> values()
meth public abstract java.util.Comparator<? super {java.util.SortedMap%0}> comparator()
meth public abstract java.util.Set<java.util.Map$Entry<{java.util.SortedMap%0},{java.util.SortedMap%1}>> entrySet()
meth public abstract java.util.Set<{java.util.SortedMap%0}> keySet()
meth public abstract java.util.SortedMap<{java.util.SortedMap%0},{java.util.SortedMap%1}> headMap({java.util.SortedMap%0})
meth public abstract java.util.SortedMap<{java.util.SortedMap%0},{java.util.SortedMap%1}> subMap({java.util.SortedMap%0},{java.util.SortedMap%0})
meth public abstract java.util.SortedMap<{java.util.SortedMap%0},{java.util.SortedMap%1}> tailMap({java.util.SortedMap%0})
meth public abstract {java.util.SortedMap%0} firstKey()
meth public abstract {java.util.SortedMap%0} lastKey()

CLSS public abstract interface java.util.SortedSet<%0 extends java.lang.Object>
intf java.util.Set<{java.util.SortedSet%0}>
meth public abstract java.util.Comparator<? super {java.util.SortedSet%0}> comparator()
meth public abstract java.util.SortedSet<{java.util.SortedSet%0}> headSet({java.util.SortedSet%0})
meth public abstract java.util.SortedSet<{java.util.SortedSet%0}> subSet({java.util.SortedSet%0},{java.util.SortedSet%0})
meth public abstract java.util.SortedSet<{java.util.SortedSet%0}> tailSet({java.util.SortedSet%0})
meth public abstract {java.util.SortedSet%0} first()
meth public abstract {java.util.SortedSet%0} last()

CLSS public abstract interface java.util.concurrent.BlockingQueue<%0 extends java.lang.Object>
intf java.util.Queue<{java.util.concurrent.BlockingQueue%0}>
meth public abstract boolean add({java.util.concurrent.BlockingQueue%0})
meth public abstract boolean contains(java.lang.Object)
meth public abstract boolean offer({java.util.concurrent.BlockingQueue%0})
meth public abstract boolean offer({java.util.concurrent.BlockingQueue%0},long,java.util.concurrent.TimeUnit) throws java.lang.InterruptedException
meth public abstract boolean remove(java.lang.Object)
meth public abstract int drainTo(java.util.Collection<? super {java.util.concurrent.BlockingQueue%0}>)
meth public abstract int drainTo(java.util.Collection<? super {java.util.concurrent.BlockingQueue%0}>,int)
meth public abstract int remainingCapacity()
meth public abstract void put({java.util.concurrent.BlockingQueue%0}) throws java.lang.InterruptedException
meth public abstract {java.util.concurrent.BlockingQueue%0} poll(long,java.util.concurrent.TimeUnit) throws java.lang.InterruptedException
meth public abstract {java.util.concurrent.BlockingQueue%0} take() throws java.lang.InterruptedException

CLSS public abstract interface java.util.concurrent.ConcurrentMap<%0 extends java.lang.Object, %1 extends java.lang.Object>
intf java.util.Map<{java.util.concurrent.ConcurrentMap%0},{java.util.concurrent.ConcurrentMap%1}>
meth public abstract boolean remove(java.lang.Object,java.lang.Object)
meth public abstract boolean replace({java.util.concurrent.ConcurrentMap%0},{java.util.concurrent.ConcurrentMap%1},{java.util.concurrent.ConcurrentMap%1})
meth public abstract {java.util.concurrent.ConcurrentMap%1} putIfAbsent({java.util.concurrent.ConcurrentMap%0},{java.util.concurrent.ConcurrentMap%1})
meth public abstract {java.util.concurrent.ConcurrentMap%1} replace({java.util.concurrent.ConcurrentMap%0},{java.util.concurrent.ConcurrentMap%1})

CLSS public abstract interface java.util.concurrent.locks.Lock
meth public abstract boolean tryLock()
meth public abstract boolean tryLock(long,java.util.concurrent.TimeUnit) throws java.lang.InterruptedException
meth public abstract java.util.concurrent.locks.Condition newCondition()
meth public abstract void lock()
meth public abstract void lockInterruptibly() throws java.lang.InterruptedException
meth public abstract void unlock()

CLSS public abstract interface java.util.concurrent.locks.ReadWriteLock
meth public abstract java.util.concurrent.locks.Lock readLock()
meth public abstract java.util.concurrent.locks.Lock writeLock()

CLSS public org.terracotta.toolkit.InvalidToolkitConfigException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr org.terracotta.toolkit.ToolkitInstantiationException

CLSS public abstract interface org.terracotta.toolkit.SecretProvider
meth public abstract byte[] getSecret()
meth public abstract void fetchSecret()

CLSS public abstract interface org.terracotta.toolkit.Toolkit
meth public abstract <%0 extends java.lang.Comparable<? super {%%0}>, %1 extends java.lang.Object> org.terracotta.toolkit.collections.ToolkitSortedMap<{%%0},{%%1}> getSortedMap(java.lang.String,java.lang.Class<{%%0}>,java.lang.Class<{%%1}>)
meth public abstract <%0 extends java.lang.Comparable<? super {%%0}>> org.terracotta.toolkit.collections.ToolkitSortedSet<{%%0}> getSortedSet(java.lang.String,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object, %1 extends java.lang.Object> org.terracotta.toolkit.collections.ToolkitMap<{%%0},{%%1}> getMap(java.lang.String,java.lang.Class<{%%0}>,java.lang.Class<{%%1}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.cache.ToolkitCache<java.lang.String,{%%0}> getCache(java.lang.String,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.cache.ToolkitCache<java.lang.String,{%%0}> getCache(java.lang.String,org.terracotta.toolkit.config.Configuration,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.collections.ToolkitBlockingQueue<{%%0}> getBlockingQueue(java.lang.String,int,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.collections.ToolkitBlockingQueue<{%%0}> getBlockingQueue(java.lang.String,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.collections.ToolkitList<{%%0}> getList(java.lang.String,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.collections.ToolkitSet<{%%0}> getSet(java.lang.String,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.events.ToolkitNotifier<{%%0}> getNotifier(java.lang.String,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.store.ToolkitStore<java.lang.String,{%%0}> getStore(java.lang.String,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.terracotta.toolkit.store.ToolkitStore<java.lang.String,{%%0}> getStore(java.lang.String,org.terracotta.toolkit.config.Configuration,java.lang.Class<{%%0}>)
meth public abstract boolean isCapabilityEnabled(java.lang.String)
meth public abstract org.terracotta.toolkit.cluster.ClusterInfo getClusterInfo()
meth public abstract org.terracotta.toolkit.concurrent.ToolkitBarrier getBarrier(java.lang.String,int)
meth public abstract org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong getAtomicLong(java.lang.String)
meth public abstract org.terracotta.toolkit.concurrent.locks.ToolkitLock getLock(java.lang.String)
meth public abstract org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock getReadWriteLock(java.lang.String)
meth public abstract void fireOperatorEvent(org.terracotta.toolkit.monitoring.OperatorEventLevel,java.lang.String,java.lang.String)
meth public abstract void shutdown()

CLSS public final !enum org.terracotta.toolkit.ToolkitCapability
fld public final static org.terracotta.toolkit.ToolkitCapability OFFHEAP
fld public final static org.terracotta.toolkit.ToolkitCapability SEARCH
meth public static org.terracotta.toolkit.ToolkitCapability getToolkitCapability(java.lang.String)
meth public static org.terracotta.toolkit.ToolkitCapability valueOf(java.lang.String)
meth public static org.terracotta.toolkit.ToolkitCapability[] values()
supr java.lang.Enum<org.terracotta.toolkit.ToolkitCapability>

CLSS public final org.terracotta.toolkit.ToolkitFactory
cons public <init>()
meth public static org.terracotta.toolkit.Toolkit createToolkit(java.lang.String) throws org.terracotta.toolkit.ToolkitInstantiationException
meth public static org.terracotta.toolkit.Toolkit createToolkit(java.lang.String,java.util.Properties) throws org.terracotta.toolkit.ToolkitInstantiationException
supr java.lang.Object
hfds TOOLKIT_URI_DELIM,TOOLKIT_URI_PREFIX
hcls ToolkitFactoryServiceLookup,ToolkitTypeSubNameTuple

CLSS public org.terracotta.toolkit.ToolkitInstantiationException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.Exception

CLSS public org.terracotta.toolkit.ToolkitRuntimeException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.RuntimeException

CLSS public abstract interface org.terracotta.toolkit.api.ToolkitFactoryService
meth public abstract boolean canHandleToolkitType(java.lang.String,java.lang.String)
meth public abstract org.terracotta.toolkit.Toolkit createToolkit(java.lang.String,java.lang.String,java.util.Properties) throws org.terracotta.toolkit.ToolkitInstantiationException

CLSS public abstract interface org.terracotta.toolkit.cache.ToolkitCache<%0 extends java.lang.Object, %1 extends java.lang.Object>
intf org.terracotta.toolkit.store.ToolkitStore<{org.terracotta.toolkit.cache.ToolkitCache%0},{org.terracotta.toolkit.cache.ToolkitCache%1}>
meth public abstract boolean isPinned({org.terracotta.toolkit.cache.ToolkitCache%0})
meth public abstract java.util.Map<{org.terracotta.toolkit.cache.ToolkitCache%0},{org.terracotta.toolkit.cache.ToolkitCache%1}> getAllQuiet(java.util.Collection<{org.terracotta.toolkit.cache.ToolkitCache%0}>)
meth public abstract void addListener(org.terracotta.toolkit.cache.ToolkitCacheListener<{org.terracotta.toolkit.cache.ToolkitCache%0}>)
meth public abstract void putNoReturn({org.terracotta.toolkit.cache.ToolkitCache%0},{org.terracotta.toolkit.cache.ToolkitCache%1},long,int,int)
meth public abstract void removeListener(org.terracotta.toolkit.cache.ToolkitCacheListener<{org.terracotta.toolkit.cache.ToolkitCache%0}>)
meth public abstract void setPinned({org.terracotta.toolkit.cache.ToolkitCache%0},boolean)
meth public abstract void unpinAll()
meth public abstract {org.terracotta.toolkit.cache.ToolkitCache%1} getQuiet(java.lang.Object)
meth public abstract {org.terracotta.toolkit.cache.ToolkitCache%1} putIfAbsent({org.terracotta.toolkit.cache.ToolkitCache%0},{org.terracotta.toolkit.cache.ToolkitCache%1},long,int,int)

CLSS public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder
cons public <init>()
meth public int getMaxTTISeconds()
meth public int getMaxTTLSeconds()
meth public int getMaxTotalCount()
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder compressionEnabled(boolean)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder concurrency(int)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder consistency(org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder copyOnReadEnabled(boolean)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder localCacheEnabled(boolean)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder maxBytesLocalHeap(long)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder maxBytesLocalOffheap(long)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder maxCountLocalHeap(int)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder maxTTISeconds(int)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder maxTTLSeconds(int)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder maxTotalCount(int)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder offheapEnabled(boolean)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder pinningStore(org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore)
meth public org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore getPinningStore()
meth public org.terracotta.toolkit.config.Configuration build()
meth public void apply(org.terracotta.toolkit.cache.ToolkitCache)
supr org.terracotta.toolkit.store.ToolkitStoreConfigBuilder
hfds maxTTISeconds,maxTTLSeconds,maxTotalCount,pinningStore

CLSS public abstract interface org.terracotta.toolkit.cache.ToolkitCacheConfigFields
fld public final static int DEFAULT_MAX_TOTAL_COUNT = 0
fld public final static int DEFAULT_MAX_TTI_SECONDS = 0
fld public final static int DEFAULT_MAX_TTL_SECONDS = 0
fld public final static int NO_MAX_TTI_SECONDS = 0
fld public final static int NO_MAX_TTL_SECONDS = 0
fld public final static java.lang.String DEFAULT_PINNING_STORE
fld public final static java.lang.String MAX_TOTAL_COUNT_FIELD_NAME = "maxTotalCount"
fld public final static java.lang.String MAX_TTI_SECONDS_FIELD_NAME = "maxTTISeconds"
fld public final static java.lang.String MAX_TTL_SECONDS_FIELD_NAME = "maxTTLSeconds"
fld public final static java.lang.String PINNING_STORE_FIELD_NAME = "pinningStore"
innr public final static !enum PinningStore
intf org.terracotta.toolkit.store.ToolkitStoreConfigFields

CLSS public final static !enum org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore
 outer org.terracotta.toolkit.cache.ToolkitCacheConfigFields
fld public final static org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore INCACHE
fld public final static org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore LOCALHEAP
fld public final static org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore LOCALMEMORY
fld public final static org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore NONE
meth public static org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore valueOf(java.lang.String)
meth public static org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore[] values()
supr java.lang.Enum<org.terracotta.toolkit.cache.ToolkitCacheConfigFields$PinningStore>

CLSS public abstract interface org.terracotta.toolkit.cache.ToolkitCacheListener<%0 extends java.lang.Object>
meth public abstract void onEviction({org.terracotta.toolkit.cache.ToolkitCacheListener%0})
meth public abstract void onExpiration({org.terracotta.toolkit.cache.ToolkitCacheListener%0})

CLSS public abstract interface org.terracotta.toolkit.cluster.ClusterEvent
innr public final static !enum Type
meth public abstract org.terracotta.toolkit.cluster.ClusterEvent$Type getType()
meth public abstract org.terracotta.toolkit.cluster.ClusterNode getNode()

CLSS public final static !enum org.terracotta.toolkit.cluster.ClusterEvent$Type
 outer org.terracotta.toolkit.cluster.ClusterEvent
fld public final static org.terracotta.toolkit.cluster.ClusterEvent$Type NODE_JOINED
fld public final static org.terracotta.toolkit.cluster.ClusterEvent$Type NODE_LEFT
fld public final static org.terracotta.toolkit.cluster.ClusterEvent$Type OPERATIONS_DISABLED
fld public final static org.terracotta.toolkit.cluster.ClusterEvent$Type OPERATIONS_ENABLED
meth public static org.terracotta.toolkit.cluster.ClusterEvent$Type valueOf(java.lang.String)
meth public static org.terracotta.toolkit.cluster.ClusterEvent$Type[] values()
supr java.lang.Enum<org.terracotta.toolkit.cluster.ClusterEvent$Type>

CLSS public abstract interface org.terracotta.toolkit.cluster.ClusterInfo
meth public abstract boolean areOperationsEnabled()
meth public abstract java.util.Set<org.terracotta.toolkit.cluster.ClusterNode> getNodes()
meth public abstract org.terracotta.toolkit.cluster.ClusterNode getCurrentNode()
meth public abstract void addClusterListener(org.terracotta.toolkit.cluster.ClusterListener)
meth public abstract void removeClusterListener(org.terracotta.toolkit.cluster.ClusterListener)

CLSS public abstract interface org.terracotta.toolkit.cluster.ClusterListener
meth public abstract void onClusterEvent(org.terracotta.toolkit.cluster.ClusterEvent)

CLSS public abstract interface org.terracotta.toolkit.cluster.ClusterNode
intf java.io.Serializable
meth public abstract java.lang.String getId()
meth public abstract java.net.InetAddress getAddress()

CLSS public abstract interface org.terracotta.toolkit.collections.ToolkitBlockingQueue<%0 extends java.lang.Object>
intf java.util.concurrent.BlockingQueue<{org.terracotta.toolkit.collections.ToolkitBlockingQueue%0}>
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitLockedObject
meth public abstract int getCapacity()

CLSS public abstract interface org.terracotta.toolkit.collections.ToolkitList<%0 extends java.lang.Object>
intf java.util.List<{org.terracotta.toolkit.collections.ToolkitList%0}>
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitLockedObject

CLSS public abstract interface org.terracotta.toolkit.collections.ToolkitMap<%0 extends java.lang.Object, %1 extends java.lang.Object>
intf java.util.concurrent.ConcurrentMap<{org.terracotta.toolkit.collections.ToolkitMap%0},{org.terracotta.toolkit.collections.ToolkitMap%1}>
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitLockedObject

CLSS public abstract interface org.terracotta.toolkit.collections.ToolkitSet<%0 extends java.lang.Object>
intf java.util.Set<{org.terracotta.toolkit.collections.ToolkitSet%0}>
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitLockedObject

CLSS public abstract interface org.terracotta.toolkit.collections.ToolkitSortedMap<%0 extends java.lang.Comparable<? super {org.terracotta.toolkit.collections.ToolkitSortedMap%0}>, %1 extends java.lang.Object>
intf java.util.SortedMap<{org.terracotta.toolkit.collections.ToolkitSortedMap%0},{org.terracotta.toolkit.collections.ToolkitSortedMap%1}>
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitLockedObject

CLSS public abstract interface org.terracotta.toolkit.collections.ToolkitSortedSet<%0 extends java.lang.Comparable<? super {org.terracotta.toolkit.collections.ToolkitSortedSet%0}>>
intf java.util.SortedSet<{org.terracotta.toolkit.collections.ToolkitSortedSet%0}>
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitLockedObject

CLSS public abstract interface org.terracotta.toolkit.concurrent.ToolkitBarrier
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitObject
meth public abstract boolean isBroken()
meth public abstract int await() throws java.lang.InterruptedException,java.util.concurrent.BrokenBarrierException
meth public abstract int await(long,java.util.concurrent.TimeUnit) throws java.lang.InterruptedException,java.util.concurrent.BrokenBarrierException,java.util.concurrent.TimeoutException
meth public abstract int getParties()
meth public abstract void reset()

CLSS public abstract interface org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitObject
meth public abstract boolean compareAndSet(long,long)
meth public abstract byte byteValue()
meth public abstract double doubleValue()
meth public abstract float floatValue()
meth public abstract int intValue()
meth public abstract long addAndGet(long)
meth public abstract long decrementAndGet()
meth public abstract long get()
meth public abstract long getAndAdd(long)
meth public abstract long getAndDecrement()
meth public abstract long getAndIncrement()
meth public abstract long getAndSet(long)
meth public abstract long incrementAndGet()
meth public abstract long longValue()
meth public abstract short shortValue()
meth public abstract void set(long)

CLSS public abstract interface org.terracotta.toolkit.concurrent.locks.ToolkitLock
intf java.util.concurrent.locks.Lock
intf org.terracotta.toolkit.object.ToolkitObject
meth public abstract boolean isHeldByCurrentThread()
meth public abstract java.util.concurrent.locks.Condition getCondition()
meth public abstract java.util.concurrent.locks.Condition newCondition()
meth public abstract org.terracotta.toolkit.concurrent.locks.ToolkitLockType getLockType()

CLSS public final !enum org.terracotta.toolkit.concurrent.locks.ToolkitLockType
fld public final static org.terracotta.toolkit.concurrent.locks.ToolkitLockType READ
fld public final static org.terracotta.toolkit.concurrent.locks.ToolkitLockType WRITE
meth public static org.terracotta.toolkit.concurrent.locks.ToolkitLockType valueOf(java.lang.String)
meth public static org.terracotta.toolkit.concurrent.locks.ToolkitLockType[] values()
supr java.lang.Enum<org.terracotta.toolkit.concurrent.locks.ToolkitLockType>

CLSS public abstract interface org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock
intf java.util.concurrent.locks.ReadWriteLock
intf org.terracotta.toolkit.object.ToolkitObject
meth public abstract org.terracotta.toolkit.concurrent.locks.ToolkitLock readLock()
meth public abstract org.terracotta.toolkit.concurrent.locks.ToolkitLock writeLock()

CLSS public abstract org.terracotta.toolkit.config.AbstractConfiguration
cons public <init>()
intf org.terracotta.toolkit.config.Configuration
meth protected abstract void internalSetConfigMapping(java.lang.String,java.io.Serializable)
meth protected java.lang.Object getMappingChecked(java.lang.String)
meth public boolean getBoolean(java.lang.String)
meth public boolean hasField(java.lang.String)
meth public int getInt(java.lang.String)
meth public java.lang.String getString(java.lang.String)
meth public long getLong(java.lang.String)
meth public org.terracotta.toolkit.config.Configuration setBoolean(java.lang.String,boolean)
meth public org.terracotta.toolkit.config.Configuration setInt(java.lang.String,int)
meth public org.terracotta.toolkit.config.Configuration setLong(java.lang.String,long)
meth public org.terracotta.toolkit.config.Configuration setObject(java.lang.String,java.io.Serializable)
meth public org.terracotta.toolkit.config.Configuration setString(java.lang.String,java.lang.String)
supr java.lang.Object

CLSS public abstract interface org.terracotta.toolkit.config.Configuration
meth public abstract boolean getBoolean(java.lang.String)
meth public abstract boolean hasField(java.lang.String)
meth public abstract int getInt(java.lang.String)
meth public abstract java.io.Serializable getObjectOrNull(java.lang.String)
meth public abstract java.lang.String getString(java.lang.String)
meth public abstract java.util.Set<java.lang.String> getKeys()
meth public abstract long getLong(java.lang.String)

CLSS public abstract !enum org.terracotta.toolkit.config.SupportedConfigurationType
fld public final static org.terracotta.toolkit.config.SupportedConfigurationType BOOLEAN
fld public final static org.terracotta.toolkit.config.SupportedConfigurationType INTEGER
fld public final static org.terracotta.toolkit.config.SupportedConfigurationType LONG
fld public final static org.terracotta.toolkit.config.SupportedConfigurationType STRING
meth public abstract <%0 extends java.lang.Object> java.io.Serializable getFromConfig(org.terracotta.toolkit.config.Configuration,java.lang.String)
meth public boolean isSupported(org.terracotta.toolkit.config.SupportedConfigurationType)
meth public static boolean isTypeSupported(java.lang.Object)
meth public static org.terracotta.toolkit.config.SupportedConfigurationType getTypeForObject(java.lang.Object)
meth public static org.terracotta.toolkit.config.SupportedConfigurationType getTypeForObjectOrNull(java.lang.Object)
meth public static org.terracotta.toolkit.config.SupportedConfigurationType valueOf(java.lang.String)
meth public static org.terracotta.toolkit.config.SupportedConfigurationType[] values()
supr java.lang.Enum<org.terracotta.toolkit.config.SupportedConfigurationType>
hfds SUPPORTED_TYPES,classType

CLSS public abstract interface org.terracotta.toolkit.events.ToolkitNotificationEvent<%0 extends java.lang.Object>
meth public abstract org.terracotta.toolkit.cluster.ClusterNode getRemoteNode()
meth public abstract {org.terracotta.toolkit.events.ToolkitNotificationEvent%0} getMessage()

CLSS public abstract interface org.terracotta.toolkit.events.ToolkitNotificationListener<%0 extends java.lang.Object>
meth public abstract void onNotification(org.terracotta.toolkit.events.ToolkitNotificationEvent<{org.terracotta.toolkit.events.ToolkitNotificationListener%0}>)

CLSS public abstract interface org.terracotta.toolkit.events.ToolkitNotifier<%0 extends java.lang.Object>
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitObject
meth public abstract java.util.List<org.terracotta.toolkit.events.ToolkitNotificationListener<{org.terracotta.toolkit.events.ToolkitNotifier%0}>> getNotificationListeners()
meth public abstract void addNotificationListener(org.terracotta.toolkit.events.ToolkitNotificationListener<{org.terracotta.toolkit.events.ToolkitNotifier%0}>)
meth public abstract void notifyListeners({org.terracotta.toolkit.events.ToolkitNotifier%0})
meth public abstract void removeNotificationListener(org.terracotta.toolkit.events.ToolkitNotificationListener<{org.terracotta.toolkit.events.ToolkitNotifier%0}>)

CLSS public abstract interface org.terracotta.toolkit.internal.TerracottaL1Instance
meth public abstract void shutdown()

CLSS public abstract interface org.terracotta.toolkit.internal.ToolkitInternal
intf org.terracotta.toolkit.Toolkit
meth public abstract java.lang.String getClientUUID()
meth public abstract org.terracotta.toolkit.concurrent.locks.ToolkitLock getLock(java.lang.String,org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal)
meth public abstract org.terracotta.toolkit.internal.ToolkitLogger getLogger(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.ToolkitProperties getProperties()
meth public abstract void registerBeforeShutdownHook(java.lang.Runnable)
meth public abstract void waitUntilAllTransactionsComplete()

CLSS public abstract interface org.terracotta.toolkit.internal.ToolkitLogger
meth public abstract boolean isDebugEnabled()
meth public abstract boolean isInfoEnabled()
meth public abstract java.lang.String getName()
meth public abstract void debug(java.lang.Object)
meth public abstract void debug(java.lang.Object,java.lang.Throwable)
meth public abstract void error(java.lang.Object)
meth public abstract void error(java.lang.Object,java.lang.Throwable)
meth public abstract void fatal(java.lang.Object)
meth public abstract void fatal(java.lang.Object,java.lang.Throwable)
meth public abstract void info(java.lang.Object)
meth public abstract void info(java.lang.Object,java.lang.Throwable)
meth public abstract void warn(java.lang.Object)
meth public abstract void warn(java.lang.Object,java.lang.Throwable)

CLSS public abstract interface org.terracotta.toolkit.internal.ToolkitProperties
meth public abstract java.lang.Boolean getBoolean(java.lang.String)
meth public abstract java.lang.Boolean getBoolean(java.lang.String,java.lang.Boolean)
meth public abstract java.lang.Integer getInteger(java.lang.String)
meth public abstract java.lang.Integer getInteger(java.lang.String,java.lang.Integer)
meth public abstract java.lang.Long getLong(java.lang.String)
meth public abstract java.lang.Long getLong(java.lang.String,java.lang.Long)
meth public abstract java.lang.String getProperty(java.lang.String)
meth public abstract java.lang.String getProperty(java.lang.String,java.lang.String)
meth public abstract void setProperty(java.lang.String,java.lang.String)

CLSS public abstract interface org.terracotta.toolkit.internal.cache.TimestampedValue
meth public abstract void updateTimestamps(int,int)

CLSS public abstract interface org.terracotta.toolkit.internal.cache.ToolkitCacheInternal<%0 extends java.lang.Object, %1 extends java.lang.Object>
intf org.terracotta.toolkit.cache.ToolkitCache<{org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%0},{org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%1}>
meth public abstract boolean containsKeyLocalOffHeap(java.lang.Object)
meth public abstract boolean containsKeyLocalOnHeap(java.lang.Object)
meth public abstract boolean containsLocalKey(java.lang.Object)
meth public abstract int localOffHeapSize()
meth public abstract int localOnHeapSize()
meth public abstract int localSize()
meth public abstract java.util.Map<java.lang.Object,java.util.Set<org.terracotta.toolkit.cluster.ClusterNode>> getNodesWithKeys(java.util.Set)
meth public abstract java.util.Set<{org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%0}> localKeySet()
meth public abstract long localOffHeapSizeInBytes()
meth public abstract long localOnHeapSizeInBytes()
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder createSearchBuilder()
meth public abstract void clearLocalCache()
meth public abstract void disposeLocally()
meth public abstract void removeAll(java.util.Set<{org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%0}>)
meth public abstract void unlockedPutNoReturn({org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%0},{org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%1},int,int,int)
meth public abstract void unlockedRemoveNoReturn(java.lang.Object)
meth public abstract {org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%1} put({org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%0},{org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%1},int,int,int)
meth public abstract {org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%1} unlockedGet(java.lang.Object,boolean)
meth public abstract {org.terracotta.toolkit.internal.cache.ToolkitCacheInternal%1} unsafeLocalGet(java.lang.Object)

CLSS public abstract interface org.terracotta.toolkit.internal.cluster.OutOfBandClusterListener
intf org.terracotta.toolkit.cluster.ClusterListener
meth public abstract boolean useOutOfBandNotification(org.terracotta.toolkit.cluster.ClusterEvent)

CLSS public final !enum org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal
fld public final static org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal CONCURRENT
fld public final static org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal READ
fld public final static org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal SYNCHRONOUS_WRITE
fld public final static org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal WRITE
meth public org.terracotta.toolkit.concurrent.locks.ToolkitLockType getToolkitLockType()
meth public static org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal valueOf(java.lang.String)
meth public static org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal[] values()
supr java.lang.Enum<org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal>

CLSS public abstract interface org.terracotta.toolkit.internal.search.SearchBuilder
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder all()
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder and()
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder attribute(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder attributeAscending(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder attributeDescending(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder average(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder beginGroup()
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder between(java.lang.String,java.lang.Object,java.lang.String,java.lang.Object,boolean,boolean)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder count()
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder endGroup()
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder greaterThan(java.lang.String,java.lang.Object)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder greaterThanEqual(java.lang.String,java.lang.Object)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder groupBy(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder ilike(java.lang.String,java.lang.Object)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder includeKeys(boolean)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder includeValues(boolean)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder lessThan(java.lang.String,java.lang.Object)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder lessThanEqual(java.lang.String,java.lang.Object)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder max(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder maxResults(int)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder min(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder notEqualTerm(java.lang.String,java.lang.Object)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder notIlike(java.lang.String,java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder or()
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder sum(java.lang.String)
meth public abstract org.terracotta.toolkit.internal.search.SearchBuilder term(java.lang.String,java.lang.Object)
meth public abstract org.terracotta.toolkit.internal.search.SearchQueryResultSet executeQuery(java.lang.String,int)

CLSS public abstract interface org.terracotta.toolkit.internal.search.SearchQueryResultSet
meth public abstract boolean anyCriteriaMatched()
meth public abstract boolean isFirstBatchPrefetched()
meth public abstract java.util.List<java.lang.Object> getAggregatorResults()
meth public abstract java.util.List<org.terracotta.toolkit.internal.search.SearchResult> getResults()

CLSS public abstract interface org.terracotta.toolkit.internal.search.SearchResult
meth public abstract java.lang.Object getValue()
meth public abstract java.lang.Object[] getSortAttributes()
meth public abstract java.lang.String getKey()
meth public abstract java.util.List<java.lang.Object> getAggregatorResults()
meth public abstract java.util.Map<java.lang.String,java.lang.Object> getAttributes()
meth public abstract java.util.Map<java.lang.String,java.lang.Object> getGroupByValues()
meth public abstract void preFetchValue()

CLSS public org.terracotta.toolkit.internal.store.ToolkitCacheConfigBuilderInternal
cons public <init>()
meth public java.lang.String getLocalStoreManagerName()
meth public org.terracotta.toolkit.internal.store.ToolkitCacheConfigBuilderInternal localStoreManagerName(java.lang.String)
supr org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder
hfds localStoreManagerName

CLSS public org.terracotta.toolkit.internal.store.ToolkitStoreConfigBuilderInternal
cons public <init>()
meth public java.lang.String getLocalStoreManagerName()
meth public org.terracotta.toolkit.internal.store.ToolkitStoreConfigBuilderInternal localStoreManagerName(java.lang.String)
supr org.terracotta.toolkit.store.ToolkitStoreConfigBuilder
hfds localStoreManagerName

CLSS public abstract interface org.terracotta.toolkit.internal.store.ToolkitStoreConfigFieldsInternal
fld public final static java.lang.String DEFAULT_LOCAL_STORE_MANAGER_NAME = ""
fld public final static java.lang.String LOCAL_STORE_MANAGER_NAME_NAME = "localStoreManagerName"

CLSS public final !enum org.terracotta.toolkit.monitoring.OperatorEventLevel
fld public final static org.terracotta.toolkit.monitoring.OperatorEventLevel CRITICAL
fld public final static org.terracotta.toolkit.monitoring.OperatorEventLevel DEBUG
fld public final static org.terracotta.toolkit.monitoring.OperatorEventLevel ERROR
fld public final static org.terracotta.toolkit.monitoring.OperatorEventLevel INFO
fld public final static org.terracotta.toolkit.monitoring.OperatorEventLevel WARN
meth public static org.terracotta.toolkit.monitoring.OperatorEventLevel valueOf(java.lang.String)
meth public static org.terracotta.toolkit.monitoring.OperatorEventLevel[] values()
supr java.lang.Enum<org.terracotta.toolkit.monitoring.OperatorEventLevel>

CLSS public abstract interface org.terracotta.toolkit.object.Destroyable
meth public abstract boolean isDestroyed()
meth public abstract void destroy()

CLSS public abstract interface org.terracotta.toolkit.object.ToolkitLockedObject
intf org.terracotta.toolkit.object.ToolkitObject
meth public abstract org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock getReadWriteLock()

CLSS public abstract interface org.terracotta.toolkit.object.ToolkitObject
meth public abstract java.lang.String getName()

CLSS public org.terracotta.toolkit.object.serialization.NotSerializableRuntimeException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr org.terracotta.toolkit.ToolkitRuntimeException

CLSS public org.terracotta.toolkit.search.SearchException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr org.terracotta.toolkit.ToolkitRuntimeException

CLSS public abstract interface org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor
fld public final static java.util.Map DO_NOT_INDEX
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor NULL_EXTRACTOR
intf java.io.Serializable
meth public abstract <%0 extends java.lang.Object, %1 extends java.lang.Object> java.util.Map<java.lang.String,java.lang.Object> attributesFor({%%0},{%%1})

CLSS public org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractorException
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
intf java.io.Serializable
supr org.terracotta.toolkit.search.SearchException
hfds serialVersionUID

CLSS public abstract !enum org.terracotta.toolkit.search.attribute.ToolkitAttributeType
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType BOOLEAN
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType BYTE
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType CHAR
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType DATE
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType DOUBLE
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType ENUM
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType FLOAT
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType INT
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType LONG
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType SHORT
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType SQL_DATE
fld public final static org.terracotta.toolkit.search.attribute.ToolkitAttributeType STRING
meth public abstract void validateValue(java.lang.String,java.lang.Object)
meth public boolean isComparable()
meth public static boolean isSupportedType(java.lang.Object)
meth public static org.terracotta.toolkit.search.attribute.ToolkitAttributeType typeFor(java.lang.String,java.lang.Object)
meth public static org.terracotta.toolkit.search.attribute.ToolkitAttributeType valueOf(java.lang.String)
meth public static org.terracotta.toolkit.search.attribute.ToolkitAttributeType[] values()
supr java.lang.Enum<org.terracotta.toolkit.search.attribute.ToolkitAttributeType>
hfds MAPPINGS

CLSS public abstract interface org.terracotta.toolkit.store.ToolkitStore<%0 extends java.lang.Object, %1 extends java.lang.Object>
intf java.util.concurrent.ConcurrentMap<{org.terracotta.toolkit.store.ToolkitStore%0},{org.terracotta.toolkit.store.ToolkitStore%1}>
intf org.terracotta.toolkit.object.Destroyable
intf org.terracotta.toolkit.object.ToolkitObject
meth public abstract boolean containsValue(java.lang.Object)
meth public abstract java.util.Map<{org.terracotta.toolkit.store.ToolkitStore%0},{org.terracotta.toolkit.store.ToolkitStore%1}> getAll(java.util.Collection<? extends {org.terracotta.toolkit.store.ToolkitStore%0}>)
meth public abstract org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock createLockForKey({org.terracotta.toolkit.store.ToolkitStore%0})
meth public abstract org.terracotta.toolkit.config.Configuration getConfiguration()
meth public abstract void putNoReturn({org.terracotta.toolkit.store.ToolkitStore%0},{org.terracotta.toolkit.store.ToolkitStore%1})
meth public abstract void removeNoReturn(java.lang.Object)
meth public abstract void setAttributeExtractor(org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor)
meth public abstract void setConfigField(java.lang.String,java.io.Serializable)

CLSS public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder
cons public <init>()
meth protected void addFieldToApply(java.lang.String,java.io.Serializable)
meth public boolean isCompressionEnabled()
meth public boolean isCopyOnReadEnabled()
meth public boolean isLocalCacheEnabled()
meth public boolean isOffheapEnabled()
meth public int getConcurrency()
meth public long getMaxBytesLocalHeap()
meth public long getMaxBytesLocalOffheap()
meth public long getMaxCountLocalHeap()
meth public org.terracotta.toolkit.config.Configuration build()
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder compressionEnabled(boolean)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder concurrency(int)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder consistency(org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder copyOnReadEnabled(boolean)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder localCacheEnabled(boolean)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder maxBytesLocalHeap(long)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder maxBytesLocalOffheap(long)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder maxCountLocalHeap(int)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigBuilder offheapEnabled(boolean)
meth public org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency getConsistency()
meth public void apply(org.terracotta.toolkit.store.ToolkitStore)
supr java.lang.Object
hfds compressionEnabled,concurrency,consistency,copyOnReadEnabled,fieldMappings,localCacheEnabled,maxBytesLocalHeap,maxBytesLocalOffheap,maxCountLocalHeap,offheapEnabled
hcls ConfigFieldMapping

CLSS public abstract interface org.terracotta.toolkit.store.ToolkitStoreConfigFields
fld public final static boolean DEFAULT_COMPRESSION_ENABLED = false
fld public final static boolean DEFAULT_COPY_ON_READ_ENABLED = false
fld public final static boolean DEFAULT_LOCAL_CACHE_ENABLED = true
fld public final static boolean DEFAULT_OFFHEAP_ENABLED = false
fld public final static int DEFAULT_CONCURRENCY = 256
fld public final static int DEFAULT_MAX_COUNT_LOCAL_HEAP = 0
fld public final static java.lang.String COMPRESSION_ENABLED_FIELD_NAME = "compressionEnabled"
fld public final static java.lang.String CONCURRENCY_FIELD_NAME = "concurrency"
fld public final static java.lang.String CONSISTENCY_FIELD_NAME = "consistency"
fld public final static java.lang.String COPY_ON_READ_ENABLED_FIELD_NAME = "copyOnReadEnabled"
fld public final static java.lang.String DEFAULT_CONSISTENCY
fld public final static java.lang.String LOCAL_CACHE_ENABLED_FIELD_NAME = "localCacheEnabled"
fld public final static java.lang.String MAX_BYTES_LOCAL_HEAP_FIELD_NAME = "maxBytesLocalHeap"
fld public final static java.lang.String MAX_BYTES_LOCAL_OFFHEAP_FIELD_NAME = "maxBytesLocalOffHeap"
fld public final static java.lang.String MAX_COUNT_LOCAL_HEAP_FIELD_NAME = "maxCountLocalHeap"
fld public final static java.lang.String OFFHEAP_ENABLED_FIELD_NAME = "offheapEnabled"
fld public final static long DEFAULT_MAX_BYTES_LOCAL_HEAP = 0
fld public final static long DEFAULT_MAX_BYTES_LOCAL_OFFHEAP = 0
innr public final static !enum Consistency

CLSS public final static !enum org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency
 outer org.terracotta.toolkit.store.ToolkitStoreConfigFields
fld public final static org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency EVENTUAL
fld public final static org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency STRONG
fld public final static org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency SYNCHRONOUS_STRONG
meth public static org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency valueOf(java.lang.String)
meth public static org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency[] values()
supr java.lang.Enum<org.terracotta.toolkit.store.ToolkitStoreConfigFields$Consistency>

CLSS public abstract interface !annotation org.terracotta.toolkit.tck.TCKStrict
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[TYPE])
intf java.lang.annotation.Annotation

