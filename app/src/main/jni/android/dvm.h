/*
 * dvm.h
 *
 *  Created on: 2016年1月1日
 *      Author: zhkl0228
 */

#ifndef DVM_H_
#define DVM_H_

#include <pthread.h>
#include <jni.h>
#include <stdio.h>
#include "common.h"
#include "dex.h"

/*
 * This defines the amount of space we leave for field slots in the
 * java.lang.Class definition.  If we alter the class to have more than
 * this many fields, the VM will abort at startup.
 */
#define CLASS_FIELD_SLOTS   4

/*
 * Some additional VM data structures that are associated with the DEX file.
 */
struct DvmDex {
    /* pointer to the DexFile we're associated with */
    DexFile*            pDexFile;

    /* clone of pDexFile->pHeader (it's used frequently enough) */
    const DexHeader*    pHeader;

    /* interned strings; parallel to "stringIds" */
    struct StringObject** pResStrings;

    /* resolved classes; parallel to "typeIds" */
    struct ClassObject** pResClasses;

    /* resolved methods; parallel to "methodIds" */
    struct Method**     pResMethods;

    /* resolved instance fields; parallel to "fieldIds" */
    /* (this holds both InstField and StaticField) */
    struct Field**      pResFields;

    /* interface method lookup cache */
    struct AtomicCache* pInterfaceCache;

    /* shared memory region with file contents */
    bool                isMappedReadOnly;
    MemMapping          memMap;

    jobject dex_object;

    /* lock ensuring mutual exclusion during updates */
    pthread_mutex_t     modLock;
};

/*
 * There are three types of objects:
 *  Class objects - an instance of java.lang.Class
 *  Array objects - an object created with a "new array" instruction
 *  Data objects - an object that is neither of the above
 *
 * We also define String objects.  At present they're equivalent to
 * DataObject, but that may change.  (Either way, they make some of the
 * code more obvious.)
 *
 * All objects have an Object header followed by type-specific data.
 */
struct Object {
    /* ptr to class object */
    void*    		clazz; // ClassObject*

    /*
     * A word containing either a "thin" lock or a "fat" monitor.  See
     * the comments in Sync.c for a description of its layout.
     */
    u4              lock;
};

union JValue {
    u1      z;
    s1      b;
    u2      c;
    s2      s;
    s4      i;
    s8      j;
    float   f;
    double  d;
    Object* l;
};

/* current state of the class, increasing as we progress */
enum ClassStatus {
    CLASS_ERROR         = -1,

    CLASS_NOTREADY      = 0,
    CLASS_IDX           = 1,    /* loaded, DEX idx in super or ifaces */
    CLASS_LOADED        = 2,    /* DEX idx values resolved */
    CLASS_RESOLVED      = 3,    /* part of linking */
    CLASS_VERIFYING     = 4,    /* in the process of being verified */
    CLASS_VERIFIED      = 5,    /* logically part of linking; done pre-init */
    CLASS_INITIALIZING  = 6,    /* class init in progress */
    CLASS_INITIALIZED   = 7,    /* ready to go */
};

/*
 * Enumeration of all the primitive types.
 */
enum PrimitiveType {
    PRIM_NOT        = 0,       /* value is a reference type, not a primitive type */
    PRIM_VOID       = 1,
    PRIM_BOOLEAN    = 2,
    PRIM_BYTE       = 3,
    PRIM_SHORT      = 4,
    PRIM_CHAR       = 5,
    PRIM_INT        = 6,
    PRIM_LONG       = 7,
    PRIM_FLOAT      = 8,
    PRIM_DOUBLE     = 9,
};

/*
 * For classes created early and thus probably in the zygote, the
 * InitiatingLoaderList is kept in gDvm. Later classes use the structure in
 * Object Class. This helps keep zygote pages shared.
 */
struct InitiatingLoaderList {
    /* a list of initiating loader Objects; grown and initialized on demand */
    Object**  initiatingLoaders;
    /* count of loaders in the above list */
    int       initiatingLoaderCount;
};

/*
 * Generic field header.  We pass this around when we want a generic Field
 * pointer (e.g. for reflection stuff).  Testing the accessFlags for
 * ACC_STATIC allows a proper up-cast.
 */
struct Field {
    ClassObject*    clazz;          /* class in which the field is declared */
    const char*     name;
    const char*     signature;      /* e.g. "I", "[C", "Landroid/os/Debug;" */
    u4              accessFlags;
};

/*
 * Instance field.
 */
struct InstField : Field {
    /*
     * This field indicates the byte offset from the beginning of the
     * (Object *) to the actual instance data; e.g., byteOffset==0 is
     * the same as the object pointer (bug!), and byteOffset==4 is 4
     * bytes farther.
     */
    int             byteOffset;
};

/*
 * Static field.
 */
struct StaticField : Field {
    JValue          value;          /* initially set from DEX for primitives */
};

/*
 * Class objects have many additional fields.  This is used for both
 * classes and interfaces, including synthesized classes (arrays and
 * primitive types).
 *
 * Class objects are unusual in that they have some fields allocated with
 * the system malloc (or LinearAlloc), rather than on the GC heap.  This is
 * handy during initialization, but does require special handling when
 * discarding java.lang.Class objects.
 *
 * The separation of methods (direct vs. virtual) and fields (class vs.
 * instance) used in Dalvik works out pretty well.  The only time it's
 * annoying is when enumerating or searching for things with reflection.
 */
struct ClassObject : Object {
    /* leave space for instance data; we could access fields directly if we
       freeze the definition of java/lang/Class */
    u4              instanceData[CLASS_FIELD_SLOTS];

    /* UTF-8 descriptor for the class; from constant pool, or on heap
       if generated ("[C") */
    const char*     descriptor;
    char*           descriptorAlloc;

    /* access flags; low 16 bits are defined by VM spec */
    u4              accessFlags;

    /* VM-unique class serial number, nonzero, set very early */
    u4              serialNumber;

    /* DexFile from which we came; needed to resolve constant pool entries */
    /* (will be NULL for VM-generated, e.g. arrays and primitive classes) */
    DvmDex*         pDvmDex;

    /* state of class initialization */
    ClassStatus     status;

    /* if class verify fails, we must return same error on subsequent tries */
    ClassObject*    verifyErrorClass;

    /* threadId, used to check for recursive <clinit> invocation */
    u4              initThreadId;

    /*
     * Total object size; used when allocating storage on gc heap.  (For
     * interfaces and abstract classes this will be zero.)
     */
    size_t          objectSize;

    /* arrays only: class object for base element, for instanceof/checkcast
       (for String[][][], this will be String) */
    ClassObject*    elementClass;

    /* arrays only: number of dimensions, e.g. int[][] is 2 */
    int             arrayDim;

    /* primitive type index, or PRIM_NOT (-1); set for generated prim classes */
    PrimitiveType   primitiveType;

    /* superclass, or NULL if this is java.lang.Object */
    ClassObject*    super;

    /* defining class loader, or NULL for the "bootstrap" system loader */
    Object*         classLoader;

    /* initiating class loader list */
    /* NOTE: for classes with low serialNumber, these are unused, and the
       values are kept in a table in gDvm. */
    InitiatingLoaderList initiatingLoaderList;

    /* array of interfaces this class implements directly */
    int             interfaceCount;
    ClassObject**   interfaces;

    /* static, private, and <init> methods */
    int             directMethodCount;
    Method*         directMethods;

    /* virtual methods defined in this class; invoked through vtable */
    int             virtualMethodCount;
    Method*         virtualMethods;

    /*
     * Virtual method table (vtable), for use by "invoke-virtual".  The
     * vtable from the superclass is copied in, and virtual methods from
     * our class either replace those from the super or are appended.
     */
    int             vtableCount;
    Method**        vtable;

    /*
     * Interface table (iftable), one entry per interface supported by
     * this class.  That means one entry for each interface we support
     * directly, indirectly via superclass, or indirectly via
     * superinterface.  This will be null if neither we nor our superclass
     * implement any interfaces.
     *
     * Why we need this: given "class Foo implements Face", declare
     * "Face faceObj = new Foo()".  Invoke faceObj.blah(), where "blah" is
     * part of the Face interface.  We can't easily use a single vtable.
     *
     * For every interface a concrete class implements, we create a list of
     * virtualMethod indices for the methods in the interface.
     */
    int             iftableCount;
    void*			 iftable; // InterfaceEntry*

    /*
     * The interface vtable indices for iftable get stored here.  By placing
     * them all in a single pool for each class that implements interfaces,
     * we decrease the number of allocations.
     */
    int             ifviPoolCount;
    int*            ifviPool;

    /* instance fields
     *
     * These describe the layout of the contents of a DataObject-compatible
     * Object.  Note that only the fields directly defined by this class
     * are listed in ifields;  fields defined by a superclass are listed
     * in the superclass's ClassObject.ifields.
     *
     * All instance fields that refer to objects are guaranteed to be
     * at the beginning of the field list.  ifieldRefCount specifies
     * the number of reference fields.
     */
    int             ifieldCount;
    int             ifieldRefCount; // number of fields that are object refs
    void*	        ifields; // InstField*

    /* bitmap of offsets of ifields */
    u4 refOffsets;

    /* source file name, if known */
    const char*     sourceFile;

    /* static fields */
    int             sfieldCount;
    StaticField     sfields[]; /* MUST be last item */
};

/*
 * Array objects have these additional fields.
 *
 * We don't currently store the size of each element.  Usually it's implied
 * by the instruction.  If necessary, the width can be derived from
 * the first char of obj->clazz->descriptor.
 */
struct ArrayObject : Object {
    /* number of elements; immutable after init */
    u4              length;

    /*
     * Array contents; actual size is (length * sizeof(type)).  This is
     * declared as u8 so that the compiler inserts any necessary padding
     * (e.g. for EABI); the actual allocation may be smaller than 8 bytes.
     */
    u8              contents[1];
};

/*
 * Native function pointer type.
 *
 * "args[0]" holds the "this" pointer for virtual methods.
 *
 * The "Bridge" form is a super-set of the "Native" form; in many places
 * they are used interchangeably.  Currently, all functions have all
 * arguments passed in, but some functions only care about the first two.
 * Passing extra arguments to a C function is (mostly) harmless.
 */
typedef void (*DalvikBridgeFunc)(const u4* args, JValue* pResult,
    const Method* method, struct Thread* self);
typedef void (*DalvikNativeFunc)(const u4* args, JValue* pResult);

/*
 * A method.  We create one of these for every method in every class
 * we load, so try to keep the size to a minimum.
 *
 * Much of this comes from and could be accessed in the data held in shared
 * memory.  We hold it all together here for speed.  Everything but the
 * pointers could be held in a shared table generated by the optimizer;
 * if we're willing to convert them to offsets and take the performance
 * hit (e.g. "meth->insns" becomes "baseAddr + meth->insnsOffset") we
 * could move everything but "nativeFunc".
 */
struct Method {
    /* the class we are a part of */
    ClassObject*    clazz;

    /* access flags; low 16 bits are defined by spec (could be u2?) */
    u4              accessFlags;

    /*
     * For concrete virtual methods, this is the offset of the method
     * in "vtable".
     *
     * For abstract methods in an interface class, this is the offset
     * of the method in "iftable[n]->methodIndexArray".
     */
    u2             methodIndex;

    /*
     * Method bounds; not needed for an abstract method.
     *
     * For a native method, we compute the size of the argument list, and
     * set "insSize" and "registerSize" equal to it.
     */
    u2              registersSize;  /* ins + locals */
    u2              outsSize;
    u2              insSize;

    /* method name, e.g. "<init>" or "eatLunch" */
    const char*     name;

    /*
     * Method prototype descriptor string (return and argument types).
     *
     * index, because generated Proxy classes don't have a DexFile.  We can
     * remove the DexFile* and reduce the size of this struct if we generate
     * a DEX for proxies.
     */
    DexProto        prototype;

    /* short-form method descriptor string */
    const char*     shorty;

    /*
     * The remaining items are not used for abstract or native methods.
     * (JNI is currently hijacking "insns" as a function pointer, set
     * after the first call.  For internal-native this stays null.)
     */

    /* the actual code */
    const u2*       insns;          /* instructions, in memory-mapped .dex */

    /* JNI: cached argument and return-type hints */
    int             jniArgInfo;

    /*
     * JNI: native method ptr; could be actual function or a JNI bridge.  We
     * don't currently discriminate between DalvikBridgeFunc and
     * DalvikNativeFunc; the former takes an argument superset (i.e. two
     * extra args) which will be ignored.  If necessary we can use
     * insns==NULL to detect JNI bridge vs. internal native.
     */
    DalvikBridgeFunc nativeFunc;

    /*
     * JNI: true if this static non-synchronized native method (that has no
     * reference arguments) needs a JNIEnv* and jclass/jobject. Libcore
     * uses this.
     */
    bool fastJni;

    /*
     * JNI: true if this method has no reference arguments. This lets the JNI
     * bridge avoid scanning the shorty for direct pointers that need to be
     * converted to local references.
     *
     */
    bool noRef;

    /*
     * JNI: true if we should log entry and exit. This is the only way
     * developers can log the local references that are passed into their code.
     * Used for debugging JNI problems in third-party code.
     */
    bool shouldTrace;

    /*
     * Register map data, if available.  This will point into the DEX file
     * if the data was computed during pre-verification, or into the
     * linear alloc area if not.
     */
    const void*	    registerMap; // RegisterMap*

    /* set if method was called during method profiling */
    bool            inProfile;
};

/*
 * Structure representing a "raw" DEX file, in its unswapped unoptimized
 * state.
 */
struct RawDexFile {
    char*       cacheFileName;
    DvmDex*     pDvmDex;
};

struct ZipArchive {
    /* open Zip archive */
    int         mFd;

    /* mapped central directory area */
    off_t       mDirectoryOffset;
    MemMapping  mDirectoryMap;

    /* number of entries in the Zip archive */
    int         mNumEntries;

    /*
     * We know how many entries are in the Zip archive, so we can have a
     * fixed-size hash table.  We probe on collisions.
     */
    int         mHashTableSize;
    void* 		mHashTable; // ZipHashEntry*
};

/*
 * This represents an open, scanned Jar file.  (It's actually for any Zip
 * archive that happens to hold a Dex file.)
 */
struct JarFile {
    ZipArchive  archive;
    //MemMapping  map;
    char*       cacheFileName;
    DvmDex*     pDvmDex;
};

/*
 * Internal struct for managing DexFile.
 */
struct DexOrJar {
    char*       fileName;
    bool        isDex;
    bool        okayToFree;
    RawDexFile* pRawDexFile;
    JarFile*    pJarFile;
    u1*         pDexMemory; // malloc()ed memory, if any
};

/*
 * How we talk to the debugger.
 */
enum JdwpTransportType {
    kJdwpTransportUnknown = 0,
    kJdwpTransportSocket,       /* transport=dt_socket */
    kJdwpTransportAndroidAdb,   /* transport=dt_android_adb */
};

/*
 * Profiler clock source.
 */
enum ProfilerClockSource {
    kProfilerClockSourceThreadCpu,
    kProfilerClockSourceWall,
    kProfilerClockSourceDual,
};

/*
 * Global DEX optimizer control.  Determines the circumstances in which we
 * try to rewrite instructions in the DEX file.
 *
 * Optimizing is performed ahead-of-time by dexopt and, in some cases, at
 * load time by the VM.
 */
enum DexOptimizerMode {
    OPTIMIZE_MODE_UNKNOWN = 0,
    OPTIMIZE_MODE_NONE,         /* never optimize (except "essential") */
    OPTIMIZE_MODE_VERIFIED,     /* only optimize verified classes (default) */
    OPTIMIZE_MODE_ALL,          /* optimize verified & unverified (risky) */
    OPTIMIZE_MODE_FULL          /* fully opt verified classes at load time */
};

/*
 * Global verification mode.  These must be in order from least verification
 * to most.  If we're using "exact GC", we may need to perform some of
 * the verification steps anyway.
 */
enum DexClassVerifyMode {
    VERIFY_MODE_UNKNOWN = 0,
    VERIFY_MODE_NONE,
    VERIFY_MODE_REMOTE,
    VERIFY_MODE_ALL
};

/*
 * Register map generation mode.  Only applicable when generateRegisterMaps
 * is enabled.  (The "disabled" state is not folded into this because
 * there are callers like dexopt that want to enable/disable without
 * specifying the configuration details.)
 *
 * "TypePrecise" is slower and requires additional storage for the register
 * maps, but allows type-precise GC.  "LivePrecise" is even slower and
 * requires additional heap during processing, but allows live-precise GC.
 */
enum RegisterMapMode {
    kRegisterMapModeUnknown = 0,
    kRegisterMapModeTypePrecise,
    kRegisterMapModeLivePrecise
};

/*
 * Execution mode, e.g. interpreter vs. JIT.
 */
enum ExecutionMode {
    kExecutionModeUnknown = 0,
    kExecutionModeInterpPortable,
    kExecutionModeInterpFast,
};

/*
 * The classpath and bootclasspath differ in that only the latter is
 * consulted when looking for classes needed by the VM.  When searching
 * for an arbitrary class definition, we start with the bootclasspath,
 * look for optional packages (a/k/a standard extensions), and then try
 * the classpath.
 *
 * In Dalvik, a class can be found in one of two ways:
 *  - in a .dex file
 *  - in a .dex file named specifically "classes.dex", which is held
 *    inside a jar file
 *
 * These two may be freely intermixed in a classpath specification.
 * Ordering is significant.
 */
enum ClassPathEntryKind {
    kCpeUnknown = 0,
    kCpeJar,
    kCpeDex,
    kCpeLastEntry       /* used as sentinel at end of array */
};

struct ClassPathEntry {
    ClassPathEntryKind kind;
    char*   fileName;
    void*   ptr;            /* JarFile* or DexFile* */
};

/*
 * Information we store for each slot in the reference table.
 */
struct IndirectRefSlot {
    Object* obj;        /* object pointer itself, NULL if the slot is unused */
    u4      serial;     /* slot serial number */
};

#define kClearedJniWeakGlobal reinterpret_cast<Object*>(0xdead1234)

class iref_iterator {
public:
    explicit iref_iterator(IndirectRefSlot* table, size_t i, size_t capacity) :
            table_(table), i_(i), capacity_(capacity) {
        skipNullsAndTombstones();
    }

    iref_iterator& operator++() {
        ++i_;
        skipNullsAndTombstones();
        return *this;
    }

    Object** operator*() {
        return &table_[i_].obj;
    }

    bool equals(const iref_iterator& rhs) const {
        return (i_ == rhs.i_ && table_ == rhs.table_);
    }

private:
    void skipNullsAndTombstones() {
        // We skip NULLs and tombstones. Clients don't want to see implementation details.
        while (i_ < capacity_ && (table_[i_].obj == NULL
                || table_[i_].obj == kClearedJniWeakGlobal)) {
            ++i_;
        }
    }

    IndirectRefSlot* table_;
    size_t i_;
    size_t capacity_;
};

union IRTSegmentState {
    u4          all;
    struct {
        u4      topIndex:16;            /* index of first unused entry */
        u4      numHoles:16;            /* #of holes in entire table */
    } parts;
};

enum IndirectRefKind {
    kIndirectKindInvalid    = 0,
    kIndirectKindLocal      = 1,
    kIndirectKindGlobal     = 2,
    kIndirectKindWeakGlobal = 3
};

typedef void* IndirectRef;

struct IndirectRefTable {
public:
    typedef iref_iterator iterator;

    /* semi-public - read/write by interpreter in native call handler */
    IRTSegmentState segmentState;

    /*
     * private:
     *
     * TODO: we can't make these private as long as the interpreter
     * uses offsetof, since private member data makes us non-POD.
     */
    /* bottom of the stack */
    IndirectRefSlot* table_;
    /* bit mask, ORed into all irefs */
    IndirectRefKind kind_;
    /* #of entries we have space for */
    size_t          alloc_entries_;
    /* max #of entries allowed */
    size_t          max_entries_;

    // TODO: want hole-filling stats (#of holes filled, total entries scanned)
    //       for performance evaluation.

    /*
     * Add a new entry.  "obj" must be a valid non-NULL object reference
     * (though it's okay if it's not fully-formed, e.g. the result from
     * dvmMalloc doesn't have obj->clazz set).
     *
     * Returns NULL if the table is full (max entries reached, or alloc
     * failed during expansion).
     */
    IndirectRef add(u4 cookie, Object* obj);

    /*
     * Given an IndirectRef in the table, return the Object it refers to.
     *
     * Returns kInvalidIndirectRefObject if iref is invalid.
     */
    Object* get(IndirectRef iref) const;

    /*
     * Returns true if the table contains a reference to this object.
     */
    bool contains(const Object* obj) const;

    /*
     * Remove an existing entry.
     *
     * If the entry is not between the current top index and the bottom index
     * specified by the cookie, we don't remove anything.  This is the behavior
     * required by JNI's DeleteLocalRef function.
     *
     * Returns "false" if nothing was removed.
     */
    bool remove(u4 cookie, IndirectRef iref);

    /*
     * Initialize an IndirectRefTable.
     *
     * If "initialCount" != "maxCount", the table will expand as required.
     *
     * "kind" should be Local or Global.  The Global table may also hold
     * WeakGlobal refs.
     *
     * Returns "false" if table allocation fails.
     */
    bool init(size_t initialCount, size_t maxCount, IndirectRefKind kind);

    /*
     * Clear out the contents, freeing allocated storage.
     *
     * You must call dvmInitReferenceTable() before you can re-use this table.
     *
     * TODO: this should be a destructor.
     */
    void destroy();

    /*
     * Dump the contents of a reference table to the log file.
     *
     * The caller should lock any external sync before calling.
     *
     * TODO: we should name the table in a constructor and remove
     * the argument here.
     */
    void dump(const char* descr) const;

    /*
     * Return the #of entries in the entire table.  This includes holes, and
     * so may be larger than the actual number of "live" entries.
     */
    size_t capacity() const {
        return segmentState.parts.topIndex;
    }

    iterator begin() {
        return iterator(table_, 0, capacity());
    }

    iterator end() {
        return iterator(table_, capacity(), capacity());
    }

private:
    static inline u4 extractIndex(IndirectRef iref) {
        u4 uref = (u4) iref;
        return (uref >> 2) & 0xffff;
    }

    static inline u4 extractSerial(IndirectRef iref) {
        u4 uref = (u4) iref;
        return uref >> 20;
    }

    static inline u4 nextSerial(u4 serial) {
        return (serial + 1) & 0xfff;
    }

    static inline IndirectRef toIndirectRef(u4 index, u4 serial, IndirectRefKind kind) {
        assert(index < 65536);
        return reinterpret_cast<IndirectRef>((serial << 20) | (index << 2) | kind);
    }
};

struct ReferenceTable {
    Object**        nextEntry;          /* top of the list */
    Object**        table;              /* bottom of the list */

    int             allocEntries;       /* #of entries we have space for */
    int             maxEntries;         /* max #of entries allowed */
};

/*
 * Holds collection of JDWP initialization parameters.
 */
struct JdwpStartupParams {
    JdwpTransportType transport;
    bool        server;
    bool        suspend;
    char        host[64];
    short       port;
    /* more will be here someday */
};

typedef u8 ObjectId;

struct ExpandBuf {
    u1*     storage;
    int     curLen;
    int     maxLen;
};

/*
 * Transport functions.
 */
struct JdwpTransport {
    bool (*startup)(struct JdwpState* state, const JdwpStartupParams* pParams);
    bool (*accept)(struct JdwpState* state);
    bool (*establish)(struct JdwpState* state);
    void (*close)(struct JdwpState* state);
    void (*shutdown)(struct JdwpState* state);
    void (*free)(struct JdwpState* state);
    bool (*isConnected)(struct JdwpState* state);
    bool (*awaitingHandshake)(struct JdwpState* state);
    bool (*processIncoming)(struct JdwpState* state);
    bool (*sendRequest)(struct JdwpState* state, ExpandBuf* pReq);
    bool (*sendBufferedRequest)(struct JdwpState* state,
        const struct iovec* iov, int iovcnt);
};

struct JdwpState {
    JdwpStartupParams   params;

    /* wait for creation of the JDWP thread */
    pthread_mutex_t threadStartLock;
    pthread_cond_t  threadStartCond;

    int             debugThreadStarted;
    pthread_t       debugThreadHandle;
    ObjectId        debugThreadId;
    bool            run;

    const JdwpTransport*    transport;
    void*   netState;

    /* for wait-for-debugger */
    pthread_mutex_t attachLock;
    pthread_cond_t  attachCond;

    /* time of last debugger activity, in milliseconds */
    s8              lastActivityWhen;

    /* global counters and a mutex to protect them */
    u4              requestSerial;
    u4              eventSerial;
    pthread_mutex_t serialLock;

    /*
     * Events requested by the debugger (breakpoints, class prep, etc).
     */
    int             numEvents;      /* #of elements in eventList */
    void*      eventList;      /* linked list of events */
    pthread_mutex_t eventLock;      /* guards numEvents/eventList */

    /*
     * Synchronize suspension of event thread (to avoid receiving "resume"
     * events before the thread has finished suspending itself).
     */
    pthread_mutex_t eventThreadLock;
    pthread_cond_t  eventThreadCond;
    ObjectId        eventThreadId;

    /*
     * DDM support.
     */
    bool            ddmActive;
};

/*
 * All fields are initialized to zero.
 *
 * Storage allocated here must be freed by a subsystem shutdown function.
 */
struct DvmGlobals {
    /*
     * Some options from the command line or environment.
     */
    char*       bootClassPathStr;
    char*       classPathStr;

    size_t      heapStartingSize;
    size_t      heapMaximumSize;
    size_t      heapGrowthLimit;
    bool        lowMemoryMode;
    double      heapTargetUtilization;
    size_t      heapMinFree;
    size_t      heapMaxFree;
    size_t      stackSize;
    size_t      mainThreadStackSize;

    bool        verboseGc;
    bool        verboseJni;
    bool        verboseClass;
    bool        verboseShutdown;

    bool        jdwpAllowed;        // debugging allowed for this process?
    bool        jdwpConfigured;     // has debugging info been provided?
    JdwpTransportType jdwpTransport;
    bool        jdwpServer;
    char*       jdwpHost;
    int         jdwpPort;
    bool        jdwpSuspend;

    ProfilerClockSource profilerClockSource;

    /*
     * Lock profiling threshold value in milliseconds.  Acquires that
     * exceed threshold are logged.  Acquires within the threshold are
     * logged with a probability of $\frac{time}{threshold}$ .  If the
     * threshold is unset no additional logging occurs.
     */
    u4          lockProfThreshold;

    int         (*vfprintfHook)(FILE*, const char*, va_list);
    void        (*exitHook)(int);
    void        (*abortHook)(void);
    bool        (*isSensitiveThreadHook)(void);

    char*       jniTrace;
    bool        reduceSignals;
    bool        noQuitHandler;
    bool        verifyDexChecksum;
    char*       stackTraceFile;     // for SIGQUIT-inspired output

    bool        logStdio;

    DexOptimizerMode    dexOptMode;
    DexClassVerifyMode  classVerifyMode;

    bool        generateRegisterMaps;
    RegisterMapMode     registerMapMode;

    bool        monitorVerification;

    bool        dexOptForSmp;

    /*
     * GC option flags.
     */
    bool        preciseGc;
    bool        preVerify;
    bool        postVerify;
    bool        concurrentMarkSweep;
    bool        verifyCardTable;
    bool        disableExplicitGc;

    int         assertionCtrlCount;
    void*	    assertionCtrl; // AssertionControl*

    ExecutionMode   executionMode;

    bool        commonInit; /* whether common stubs are generated */
    bool        constInit; /* whether global constants are initialized */

    /*
     * VM init management.
     */
    bool        initializing;
    bool        optimizing;

    /*
     * java.lang.System properties set from the command line with -D.
     * This is effectively a set, where later entries override earlier
     * ones.
     */
    void* 		properties; // std::vector<std::string>

    /*
     * Where the VM goes to find system classes.
     */
    ClassPathEntry* bootClassPath;
    /* used by the DEX optimizer to load classes from an unfinished DEX */
    DvmDex*     bootClassPathOptExtra;
    bool        optimizingBootstrapClass;

    /*
     * Loaded classes, hashed by class name.  Each entry is a ClassObject*,
     * allocated in GC space.
     */
    void*	  loadedClasses; // HashTable*

    /*
     * Value for the next class serial number to be assigned.  This is
     * incremented as we load classes.  Failed loads and races may result
     * in some numbers being skipped, and the serial number is not
     * guaranteed to start at 1, so the current value should not be used
     * as a count of loaded classes.
     */
    volatile int classSerialNumber;

    /*
     * Classes with a low classSerialNumber are probably in the zygote, and
     * their InitiatingLoaderList is not used, to promote sharing. The list is
     * kept here instead.
     */
    InitiatingLoaderList* initiatingLoaderList;

    /*
     * Interned strings.
     */

    /* A mutex that guards access to the interned string tables. */
    pthread_mutex_t internLock;

    /* Hash table of strings interned by the user. */
    void*   	    internedStrings; // HashTable*

    /* Hash table of strings interned by the class loader. */
    void*		    literalStrings; // HashTable*

    /*
     * Classes constructed directly by the vm.
     */

    /* the class Class */
    ClassObject* classJavaLangClass;

    /* synthetic classes representing primitive types */
    ClassObject* typeVoid;
    ClassObject* typeBoolean;
    ClassObject* typeByte;
    ClassObject* typeShort;
    ClassObject* typeChar;
    ClassObject* typeInt;
    ClassObject* typeLong;
    ClassObject* typeFloat;
    ClassObject* typeDouble;

    /* synthetic classes for arrays of primitives */
    ClassObject* classArrayBoolean;
    ClassObject* classArrayByte;
    ClassObject* classArrayShort;
    ClassObject* classArrayChar;
    ClassObject* classArrayInt;
    ClassObject* classArrayLong;
    ClassObject* classArrayFloat;
    ClassObject* classArrayDouble;

    /*
     * Quick lookups for popular classes used internally.
     */
    ClassObject* classJavaLangClassArray;
    ClassObject* classJavaLangClassLoader;
    ClassObject* classJavaLangObject;
    ClassObject* classJavaLangObjectArray;
    ClassObject* classJavaLangString;
    ClassObject* classJavaLangThread;
    ClassObject* classJavaLangVMThread;
    ClassObject* classJavaLangThreadGroup;
    ClassObject* classJavaLangStackTraceElement;
    ClassObject* classJavaLangStackTraceElementArray;
    ClassObject* classJavaLangAnnotationAnnotationArray;
    ClassObject* classJavaLangAnnotationAnnotationArrayArray;
    ClassObject* classJavaLangReflectAccessibleObject;
    ClassObject* classJavaLangReflectConstructor;
    ClassObject* classJavaLangReflectConstructorArray;
    ClassObject* classJavaLangReflectField;
    ClassObject* classJavaLangReflectFieldArray;
    ClassObject* classJavaLangReflectMethod;
    ClassObject* classJavaLangReflectMethodArray;
    ClassObject* classJavaLangReflectProxy;
    ClassObject* classJavaLangSystem;
    ClassObject* classJavaNioDirectByteBuffer;
    ClassObject* classLibcoreReflectAnnotationFactory;
    ClassObject* classLibcoreReflectAnnotationMember;
    ClassObject* classLibcoreReflectAnnotationMemberArray;
    ClassObject* classOrgApacheHarmonyDalvikDdmcChunk;
    ClassObject* classOrgApacheHarmonyDalvikDdmcDdmServer;
    ClassObject* classJavaLangRefFinalizerReference;

    /*
     * classes representing exception types. The names here don't include
     * packages, just to keep the use sites a bit less verbose. All are
     * in java.lang, except where noted.
     */
    ClassObject* exAbstractMethodError;
    ClassObject* exArithmeticException;
    ClassObject* exArrayIndexOutOfBoundsException;
    ClassObject* exArrayStoreException;
    ClassObject* exClassCastException;
    ClassObject* exClassCircularityError;
    ClassObject* exClassFormatError;
    ClassObject* exClassNotFoundException;
    ClassObject* exError;
    ClassObject* exExceptionInInitializerError;
    ClassObject* exFileNotFoundException; /* in java.io */
    ClassObject* exIOException;           /* in java.io */
    ClassObject* exIllegalAccessError;
    ClassObject* exIllegalAccessException;
    ClassObject* exIllegalArgumentException;
    ClassObject* exIllegalMonitorStateException;
    ClassObject* exIllegalStateException;
    ClassObject* exIllegalThreadStateException;
    ClassObject* exIncompatibleClassChangeError;
    ClassObject* exInstantiationError;
    ClassObject* exInstantiationException;
    ClassObject* exInternalError;
    ClassObject* exInterruptedException;
    ClassObject* exLinkageError;
    ClassObject* exNegativeArraySizeException;
    ClassObject* exNoClassDefFoundError;
    ClassObject* exNoSuchFieldError;
    ClassObject* exNoSuchFieldException;
    ClassObject* exNoSuchMethodError;
    ClassObject* exNullPointerException;
    ClassObject* exOutOfMemoryError;
    ClassObject* exRuntimeException;
    ClassObject* exStackOverflowError;
    ClassObject* exStaleDexCacheError;    /* in dalvik.system */
    ClassObject* exStringIndexOutOfBoundsException;
    ClassObject* exThrowable;
    ClassObject* exTypeNotPresentException;
    ClassObject* exUnsatisfiedLinkError;
    ClassObject* exUnsupportedOperationException;
    ClassObject* exVerifyError;
    ClassObject* exVirtualMachineError;

    /* method offsets - Object */
    int         voffJavaLangObject_equals;
    int         voffJavaLangObject_hashCode;
    int         voffJavaLangObject_toString;

    /* field offsets - String */
    int         offJavaLangString_value;
    int         offJavaLangString_count;
    int         offJavaLangString_offset;
    int         offJavaLangString_hashCode;

    /* field offsets - Thread */
    int         offJavaLangThread_vmThread;
    int         offJavaLangThread_group;
    int         offJavaLangThread_daemon;
    int         offJavaLangThread_name;
    int         offJavaLangThread_priority;
    int         offJavaLangThread_uncaughtHandler;
    int         offJavaLangThread_contextClassLoader;

    /* method offsets - Thread */
    int         voffJavaLangThread_run;

    /* field offsets - ThreadGroup */
    int         offJavaLangThreadGroup_name;
    int         offJavaLangThreadGroup_parent;

    /* field offsets - VMThread */
    int         offJavaLangVMThread_thread;
    int         offJavaLangVMThread_vmData;

    /* method offsets - ThreadGroup */
    int         voffJavaLangThreadGroup_removeThread;

    /* field offsets - Throwable */
    int         offJavaLangThrowable_stackState;
    int         offJavaLangThrowable_cause;

    /* method offsets - ClassLoader */
    int         voffJavaLangClassLoader_loadClass;

    /* direct method pointers - ClassLoader */
    Method*     methJavaLangClassLoader_getSystemClassLoader;

    /* field offsets - java.lang.reflect.* */
    int         offJavaLangReflectConstructor_slot;
    int         offJavaLangReflectConstructor_declClass;
    int         offJavaLangReflectField_slot;
    int         offJavaLangReflectField_declClass;
    int         offJavaLangReflectMethod_slot;
    int         offJavaLangReflectMethod_declClass;

    /* field offsets - java.lang.ref.Reference */
    int         offJavaLangRefReference_referent;
    int         offJavaLangRefReference_queue;
    int         offJavaLangRefReference_queueNext;
    int         offJavaLangRefReference_pendingNext;

    /* field offsets - java.lang.ref.FinalizerReference */
    int offJavaLangRefFinalizerReference_zombie;

    /* method pointers - java.lang.ref.ReferenceQueue */
    Method* methJavaLangRefReferenceQueueAdd;

    /* method pointers - java.lang.ref.FinalizerReference */
    Method* methJavaLangRefFinalizerReferenceAdd;

    /* constructor method pointers; no vtable involved, so use Method* */
    Method*     methJavaLangStackTraceElement_init;
    Method*     methJavaLangReflectConstructor_init;
    Method*     methJavaLangReflectField_init;
    Method*     methJavaLangReflectMethod_init;
    Method*     methOrgApacheHarmonyLangAnnotationAnnotationMember_init;

    /* static method pointers - android.lang.annotation.* */
    Method*
        methOrgApacheHarmonyLangAnnotationAnnotationFactory_createAnnotation;

    /* direct method pointers - java.lang.reflect.Proxy */
    Method*     methJavaLangReflectProxy_constructorPrototype;

    /* field offsets - java.lang.reflect.Proxy */
    int         offJavaLangReflectProxy_h;

    /* direct method pointer - java.lang.System.runFinalization */
    Method*     methJavaLangSystem_runFinalization;

    /* field offsets - java.io.FileDescriptor */
    int         offJavaIoFileDescriptor_descriptor;

    /* direct method pointers - dalvik.system.NativeStart */
    Method*     methDalvikSystemNativeStart_main;
    Method*     methDalvikSystemNativeStart_run;

    /* assorted direct buffer helpers */
    Method*     methJavaNioDirectByteBuffer_init;
    int         offJavaNioBuffer_capacity;
    int         offJavaNioBuffer_effectiveDirectAddress;

    /* direct method pointers - org.apache.harmony.dalvik.ddmc.DdmServer */
    Method*     methDalvikDdmcServer_dispatch;
    Method*     methDalvikDdmcServer_broadcast;

    /* field offsets - org.apache.harmony.dalvik.ddmc.Chunk */
    int         offDalvikDdmcChunk_type;
    int         offDalvikDdmcChunk_data;
    int         offDalvikDdmcChunk_offset;
    int         offDalvikDdmcChunk_length;

    /*
     * Thread list.  This always has at least one element in it (main),
     * and main is always the first entry.
     *
     * The threadListLock is used for several things, including the thread
     * start condition variable.  Generally speaking, you must hold the
     * threadListLock when:
     *  - adding/removing items from the list
     *  - waiting on or signaling threadStartCond
     *  - examining the Thread struct for another thread (this is to avoid
     *    one thread freeing the Thread struct while another thread is
     *    perusing it)
     */
    Thread*     threadList;
    pthread_mutex_t threadListLock;

    pthread_cond_t threadStartCond;

    /*
     * The thread code grabs this before suspending all threads.  There
     * are a few things that can cause a "suspend all":
     *  (1) the GC is starting;
     *  (2) the debugger has sent a "suspend all" request;
     *  (3) a thread has hit a breakpoint or exception that the debugger
     *      has marked as a "suspend all" event;
     *  (4) the SignalCatcher caught a signal that requires suspension.
     *  (5) (if implemented) the JIT needs to perform a heavyweight
     *      rearrangement of the translation cache or JitTable.
     *
     * Because we use "safe point" self-suspension, it is never safe to
     * do a blocking "lock" call on this mutex -- if it has been acquired,
     * somebody is probably trying to put you to sleep.  The leading '_' is
     * intended as a reminder that this lock is special.
     */
    pthread_mutex_t _threadSuspendLock;

    /*
     * Guards Thread->suspendCount for all threads, and
     * provides the lock for the condition variable that all suspended threads
     * sleep on (threadSuspendCountCond).
     *
     * This has to be separate from threadListLock because of the way
     * threads put themselves to sleep.
     */
    pthread_mutex_t threadSuspendCountLock;

    /*
     * Suspended threads sleep on this.  They should sleep on the condition
     * variable until their "suspend count" is zero.
     *
     * Paired with "threadSuspendCountLock".
     */
    pthread_cond_t  threadSuspendCountCond;

    /*
     * Sum of all threads' suspendCount fields. Guarded by
     * threadSuspendCountLock.
     */
    int  sumThreadSuspendCount;

    /*
     * MUTEX ORDERING: when locking multiple mutexes, always grab them in
     * this order to avoid deadlock:
     *
     *  (1) _threadSuspendLock      (use lockThreadSuspend())
     *  (2) threadListLock          (use dvmLockThreadList())
     *  (3) threadSuspendCountLock  (use lockThreadSuspendCount())
     */


    /*
     * Thread ID bitmap.  We want threads to have small integer IDs so
     * we can use them in "thin locks".
     */
    void*		  threadIdMap; // BitVector*

    /*
     * Manage exit conditions.  The VM exits when all non-daemon threads
     * have exited.  If the main thread returns early, we need to sleep
     * on a condition variable.
     */
    int         nonDaemonThreadCount;   /* must hold threadListLock to access */
    pthread_cond_t  vmExitCond;

    /*
     * The set of DEX files loaded by custom class loaders.
     */
    void*  userDexFiles; // HashTable*

    /*
	 * JNI global reference table.
	 */
	IndirectRefTable jniGlobalRefTable;
	IndirectRefTable jniWeakGlobalRefTable;
	pthread_mutex_t jniGlobalRefLock;
	pthread_mutex_t jniWeakGlobalRefLock;

	/*
	 * JNI pinned object table (used for primitive arrays).
	 */
	ReferenceTable  jniPinRefTable;
	pthread_mutex_t jniPinRefLock;

	/*
	 * Native shared library table.
	 */
	void*  nativeLibs;

	/*
	 * GC heap lock.  Functions like gcMalloc() acquire this before making
	 * any changes to the heap.  It is held throughout garbage collection.
	 */
	pthread_mutex_t gcHeapLock;

	/*
	 * Condition variable to queue threads waiting to retry an
	 * allocation.  Signaled after a concurrent GC is completed.
	 */
	pthread_cond_t gcHeapCond;

	/* Opaque pointer representing the heap. */
	void*     gcHeap;

	/* The card table base, modified as needed for marking cards. */
	u1*         biasedCardTableBase;

	/*
	 * Pre-allocated throwables.
	 */
	Object*     outOfMemoryObj;
	Object*     internalErrorObj;
	Object*     noClassDefFoundErrorObj;

	/* Monitor list, so we can free them */
	/*volatile*/ void* monitorList;

	/* Monitor for Thread.sleep() implementation */
	void*    threadSleepMon;

	/* set when we create a second heap inside the zygote */
	bool        newZygoteHeapAllocated;

	/*
	 * TLS keys.
	 */
	pthread_key_t pthreadKeySelf;       /* Thread*, for dvmThreadSelf */

	/*
	 * Cache results of "A instanceof B".
	 */
	AtomicCache* instanceofCache;

	/* inline substitution table, used during optimization */
	void*          inlineSubs;

	/*
	 * Bootstrap class loader linear allocator.
	 */
	void* pBootLoaderAlloc;

	/*
	 * Compute some stats on loaded classes.
	 */
	int         numLoadedClasses;
	int         numDeclaredMethods;
	int         numDeclaredInstFields;
	int         numDeclaredStaticFields;

	/* when using a native debugger, set this to suppress watchdog timers */
	bool        nativeDebuggerActive;

	/*
	 * JDWP debugger support.
	 *
	 * Note: Each thread will normally determine whether the debugger is active
	 * for it by referring to its subMode flags.  "debuggerActive" here should be
	 * seen as "debugger is making requests of 1 or more threads".
	 */
	bool        debuggerConnected;      /* debugger or DDMS is connected */
	bool        debuggerActive;         /* debugger is making requests */
	JdwpState*  jdwpState;
};

extern "C" {
	bool dvmInitClass(ClassObject* clazz);

	/*
	 * The interpreter just threw.  Handle any special subMode requirements.
	 * All interpSave state must be valid on entry.
	 */
	void dvmReportExceptionThrow(Thread* self, Object* exception);

	/*
	 * The interpreter is preparing to do an invoke (both native & normal).
	 * Handle any special subMode requirements.  All interpSave state
	 * must be valid on entry.
	 */
	void dvmReportInvoke(Thread* self, const Method* methodToCall);

	/*
	 * The interpreter is preparing to do a native invoke. Handle any
	 * special subMode requirements.  NOTE: for a native invoke,
	 * dvmReportInvoke() and dvmReportPreNativeInvoke() will both
	 * be called prior to the invoke.  fp is the Dalvik FP of the calling
	 * method.
	 */
	void dvmReportPreNativeInvoke(const Method* methodToCall, Thread* self, u4* fp);

	/*
	 * The interpreter has returned from a native invoke. Handle any
	 * special subMode requirements.  fp is the Dalvik FP of the calling
	 * method.
	 */
	void dvmReportPostNativeInvoke(const Method* methodToCall, Thread* self, u4* fp);

	/*
	 * The interpreter has returned from a normal method.  Handle any special
	 * subMode requirements.  All interpSave state must be valid on entry.
	 */
	void dvmReportReturn(Thread* self);
}

/*
 * Decode a local, global, or weak-global reference.
 */
extern "C++" {
	Object* dvmDecodeIndirectRef(Thread* self, jobject jobj);

	Thread* dvmThreadSelf();
	/*
	 * Load the named class (by descriptor) from the specified DEX file.
	 * Used by class loaders to instantiate a class object from a
	 * VM-managed DEX.
	 */
	ClassObject* dvmDefineClass(DvmDex* pDvmDex, const char* descriptor,
	    Object* classLoader);

	/**
	 * Throw an IllegalArgumentException in the current thread, with the
	 * given detail message.
	 */
	void dvmThrowIllegalArgumentException(const char* msg);

	/*
	 * Convert slot numbers back to objects.
	 */
	Field* dvmSlotToField(ClassObject* clazz, int slot);
	Method* dvmSlotToMethod(ClassObject* clazz, int slot);
}

/*
 * Determine if a class has been initialized.
 */
inline bool dvmIsClassInitialized(const ClassObject* clazz) {
    return (clazz->status == CLASS_INITIALIZED);
}

/* Get whether the given method has associated bytecode. This is the
 * case for methods which are neither native nor abstract. */
inline bool dvmIsBytecodeMethod(const Method* method) {
    return (method->accessFlags & (ACC_NATIVE | ACC_ABSTRACT)) == 0;
}

/*
 * Get the associated code struct for a method. This returns NULL
 * for non-bytecode methods.
 */
inline const DexCode* dvmGetMethodCode(const Method* meth) {
    if (dvmIsBytecodeMethod(meth)) {
        /*
         * The insns field for a bytecode method actually points at
         * &(DexCode.insns), so we can subtract back to get at the
         * DexCode in front.
         */
        return (const DexCode*)
            (((const u1*) meth->insns) - offsetof(DexCode, insns));
    } else {
        return NULL;
    }
}

/* get full path of optimized DEX file */
inline const char* dvmGetJarFileCacheFileName(JarFile* pJarFile) {
    return pJarFile->cacheFileName;
}

/* get full path of optimized DEX file */
inline const char* dvmGetRawDexFileCacheFileName(RawDexFile* pRawDexFile) {
    return pRawDexFile->cacheFileName;
}

inline bool dvmIsNativeMethod(const Method* method) {
    return (method->accessFlags & ACC_NATIVE) != 0;
}

inline bool dvmIsAbstractMethod(const Method* method) {
    return (method->accessFlags & ACC_ABSTRACT) != 0;
}

#endif /* DVM_H_ */
