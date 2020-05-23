#!/bin/bash
set -e

cd $(dirname $0)

readonly JNI_LIBS="$PWD/app/src/main/jniLibs"
readonly NDK_PATH="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
export PATH="$PATH:$NDK_PATH"

rm -rf $JNI_LIBS
mkdir -pv $JNI_LIBS/{armeabi-v7a,arm64-v8a,x86,x86_64}

cd $PWD/html2md

echo "Setting up cargo configuration"
mkdir -pv .cargo
cat <<EOF > .cargo/config
[target.aarch64-linux-android]
ar = "$NDK_PATH/aarch64-linux-android-ar"
linker = "$NDK_PATH/aarch64-linux-android21-clang"

[target.armv7-linux-androideabi]
ar = "$NDK_PATH/arm-linux-androideabi-ar"
linker = "$NDK_PATH/armv7a-linux-androideabi21-clang"

[target.i686-linux-android]
ar = "$NDK_PATH/i686-linux-android-ar"
linker = "$NDK_PATH/i686-linux-android21-clang"

[target.x86_64-linux-android]
ar = "$NDK_PATH/x86_64-linux-android-ar"
linker = "$NDK_PATH/x86_64-linux-android21-clang"
EOF

echo "Building for armv7..."
export CC=armv7a-linux-androideabi21-clang
export CXX=armv7a-linux-androideabi21-clang++
cargo build --lib --release --target armv7-linux-androideabi
cp -v target/armv7-linux-androideabi/release/libhtml2md.so $JNI_LIBS/armeabi-v7a/libhtml2md.so

echo "Building for aarch64..."
export CC=aarch64-linux-android21-clang
export CXX=aarch64-linux-android21-clang++
cargo build --lib --release --target aarch64-linux-android
cp -v target/aarch64-linux-android/release/libhtml2md.so $JNI_LIBS/arm64-v8a/libhtml2md.so

echo "Building for i686..."
export CC=i686-linux-android21-clang
export CXX=i686-linux-android21-clang++
cargo build --lib --release --target i686-linux-android
cp -v target/i686-linux-android/release/libhtml2md.so $JNI_LIBS/x86/libhtml2md.so

echo "Building for v86_64..."
export CC=x86_64-linux-android21-clang
export CXX=x86_64-linux-android21-clang++
cargo build --lib --release --target x86_64-linux-android
cp -v target/x86_64-linux-android/release/libhtml2md.so $JNI_LIBS/x86_64/libhtml2md.so


