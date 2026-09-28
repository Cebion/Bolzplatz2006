#!/bin/bash
# Builds the aarch64 PortMaster artifacts for Bolzplatz 2006 into portmaster/out/:
#   game.jar, lib/irrlicht.jar, libs.aarch64/libirrlicht_wrap.so
# Needs: g++, libx11-dev, libxxf86vm-dev, libxext-dev, libgl-dev, libglu1-mesa-dev,
#        openjdk-17-jdk-headless, unzip, and vecmath.jar (libvecmath-java) at $VECMATH_JAR.
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/portmaster/build"
OUT="$ROOT/portmaster/out"
VECMATH_JAR="${VECMATH_JAR:-/usr/share/java/vecmath.jar}"
JDK_HOME="${JDK_HOME:-$(dirname "$(dirname "$(readlink -f "$(which javac)")")")}"

rm -rf "$WORK" "$OUT"
mkdir -p "$WORK" "$OUT/lib" "$OUT/libs.aarch64"
cp -r "$ROOT/libs/irrlicht-1.2-patched" "$WORK/irrlicht"

# Irrlicht 1.2 (patched): modern GCC fixes, crowd scene node added to the object list
cd "$WORK/irrlicht/source/Irrlicht"
sed -i '160s/return false;/return 0;/' C3DSMeshFileLoader.cpp
sed -i '96s/return false;/return 0;/;102s/return false;/return 0;/' CDMFLoader.cpp
sed -i '98s/return false;/return 0;/;102s/return false;/return 0;/' COgreMeshFileLoader.cpp
sed -i '78s/return false;/return 0;/' COBJMeshFileLoader.cpp
sed -i '116s/return false;/return 0;/' COCTLoader.cpp
sed -i '5i #include <stdio.h>\n#include <stdlib.h>\n#include <string.h>\n#include <sys/types.h>\n#include <dirent.h>\n#include <sys/stat.h>\n#include <unistd.h>' CFileList.cpp
# png_uint_32 is 64-bit on aarch64 in libpng 1.2.8; read IHDR into real png_uint_32 temporaries
perl -0 -i -pe 's/png_get_IHDR\(png_ptr, info_ptr,\s*\(png_uint_32\*\)&Width, \(png_uint_32\*\)&Height,\s*&BitDepth, &ColorType, NULL, NULL, NULL\);/{ png_uint_32 w32, h32; png_get_IHDR(png_ptr, info_ptr, &w32, &h32, &BitDepth, &ColorType, NULL, NULL, NULL); Width = w32; Height = h32; }/g' CImageLoaderPNG.cpp
grep -q '(png_uint_32\*)&Width' CImageLoaderPNG.cpp && { echo "PNG IHDR patch failed"; exit 1; }
# unbind the texture before swapping, so gl4es submits the frame's last batched immediate-mode
# draws (Crusty's GLX swap doesn't flush them); much cheaper than glFlush on Mali
sed -i 's/^\tglXSwapBuffers(XDisplay, XWindow);/\tsetTexture(0, 0);\n\tglXSwapBuffers(XDisplay, XWindow);/' COpenGLDriver.cpp
grep -B1 'glXSwapBuffers(XDisplay, XWindow);' COpenGLDriver.cpp | grep -q 'setTexture(0, 0);' || { echo "swap flush patch failed"; exit 1; }
# drop the glFlush after every mesh draw call; on tile-based GPUs each one forces a full framebuffer flush
sed -i '/^\tglFlush();\r\?$/d' COpenGLDriver.cpp
grep -q 'glFlush' COpenGLDriver.cpp && { echo "glFlush removal failed"; exit 1; }
mkdir -p ../../lib/Linux
make -j"$(nproc)" staticlib EXTRAOBJ=CCrowdSceneNode.o \
  CXXFLAGS="-Wno-reorder -fPIC -O2 -fpermissive -w" CFLAGS="-O2 -fPIC -w"

# JIRR: pre-generated SWIG 1.3 wrapper matching Irrlicht 1.2
cd "$WORK/irrlicht/jirr-dev"
unzip -q "src - check if this is the output of JIRR.zip"
perl -i -pe 'if (/^  public EKEY_CODE getKeyInputKey\(\) \{/) { print "  public int getKeyInputKeyInt() {\n    return JirrJNI.SEvent_getKeyInputKey(swigCPtr);\n  }\n\n"; }' \
  src/java/net/sf/jirr/SEvent.java
mkdir -p classes
javac -nowarn --release 8 -encoding ISO-8859-1 -d classes $(find src/java -name '*.java')
(cd classes && jar cf "$OUT/lib/irrlicht.jar" net)

g++ -O2 -fPIC -fpermissive -w -I"$JDK_HOME/include" -I"$JDK_HOME/include/linux" -I../include -I. \
  -c src/native/irrlicht_wrap.cxx -o irrlicht_wrap.o
gcc -O2 -fPIC -c "$ROOT/portmaster/glibc_compat.c" -o glibc_compat.o
g++ -shared -o "$OUT/libs.aarch64/libirrlicht_wrap.so" irrlicht_wrap.o glibc_compat.o \
  ../source/Irrlicht/libIrrlicht.a -Wl,--no-undefined -Wl,--wrap=stat \
  -lX11 -lXxf86vm -lXext -lGL -lGLU
strip "$OUT/libs.aarch64/libirrlicht_wrap.so"

# game.jar (editor tools excluded)
cd "$ROOT/game"
CP="$VECMATH_JAR:$OUT/lib/irrlicht.jar:lib/lwjgl/lwjgl.jar:lib/sdl/sdljava.jar:$(ls lib/vorbisspi/*.jar | tr '\n' ':')"
find src -name '*.java' | grep -v -E "PlayerX.java|TeamAI[1-4].java|TeamAIPlayer1.java|StreamPlaylist.java" > "$WORK/srcs.txt"
mkdir -p "$WORK/classes"
javac -nowarn --release 8 -encoding ISO-8859-1 -cp "$CP" -d "$WORK/classes" @"$WORK/srcs.txt"
(cd "$WORK/classes" && jar cf "$OUT/game.jar" $(find com -type f -name '*.class' -not -path 'com/xenoage/bp2k6/tools/*'))

echo "Build finished: $OUT"
