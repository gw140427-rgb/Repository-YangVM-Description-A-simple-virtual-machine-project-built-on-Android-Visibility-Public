# QEMU 최신 공식 문서 참고자료

**확인 기준일:** 2026-10-18  
**목적:** Android/Termux 환경에서 QEMU를 참고하고 Debian·Ubuntu 가상머신 선택/부팅 프로젝트를 설계하기 위한 공식 자료 모음.

> `/docs/master/`는 QEMU 공식 문서의 최신 개발 문서 트리이며 계속 갱신될 수 있습니다. 특정 안정판 릴리스 문서와 같다고 보장되지는 않습니다. 이 문서는 공식 문서를 참고해 정리한 자료이며, 이 저장소에서 QEMU를 빌드하거나 Termux 설치를 시험했다는 의미는 아닙니다.

## 공식 문서

1. **QEMU System Emulation 소개**  
   https://www.qemu.org/docs/master/system/introduction.html  
   QEMU 시스템 에뮬레이션은 CPU, 메모리 및 장치를 모델링해 게스트 운영체제를 실행합니다. TCG는 소프트웨어 에뮬레이션이고, KVM 등 하드웨어 가속 사용 가능 여부는 호스트 환경에 달려 있습니다.

2. **QEMU 실행 옵션**  
   https://www.qemu.org/docs/master/system/invocation.html  
   머신 유형, CPU, 메모리, 장치, 가속기, 부팅 방식 등 실행 옵션을 설명합니다. 실제 지원 옵션은 설치된 QEMU 바이너리와 대상 아키텍처에 따라 확인해야 합니다.

3. **QEMU 디스크 이미지**  
   https://www.qemu.org/docs/master/system/images.html  
   가상 디스크 이미지 형식 및 사용 방법을 설명합니다. 테스트에는 Android 시스템이나 실제 장치 저장장치 대신 별도의 가상 디스크 파일을 사용하세요.

4. **QEMU 시스템 에뮬레이터 대상**  
   https://www.qemu.org/docs/master/system/targets.html  
   게스트 CPU 아키텍처에 맞는 에뮬레이터와 아키텍처별 문서를 찾는 출발점입니다.

5. **Termux 공식 패키지 저장소**  
   https://github.com/termux/termux-packages  
   Termux 패키지 빌드 정의와 패치를 관리하는 저장소입니다. 현재 기기에서 설치 가능한 패키지 이름과 버전은 Termux의 활성 저장소에서 확인해야 합니다. 이 문서는 특정 QEMU 패키지 이름이나 설치 성공을 보증하지 않습니다.

## Debian·Ubuntu 멀티부팅 설계 시 구분할 점

- **QEMU와 부팅 메뉴는 별개입니다.** QEMU는 가상 머신을 실행합니다. 여러 게스트 중 하나를 고르는 메뉴는 별도로 구현해야 합니다.
- **게스트별 디스크를 분리합니다.** Debian과 Ubuntu는 각각 독립된 가상 디스크 파일을 사용하도록 설계하면 실험 간 충돌을 줄일 수 있습니다.
- **아키텍처를 확인합니다.** ARM64 게스트와 x86/x86_64 게스트는 다른 대상입니다. 대상에 맞는 QEMU 시스템 실행 파일, 펌웨어, 설치 이미지 및 장치 구성이 필요합니다.
- **성능은 보장되지 않습니다.** 소프트웨어 에뮬레이션은 특히 호스트와 게스트 아키텍처가 다를 때 느릴 수 있습니다. 메모리와 저장 공간도 휴대폰의 여유 자원을 고려해 배정해야 합니다.
- **호스트 저장장치를 직접 연결하지 않습니다.** 실제 Android 파티션이나 중요한 저장장치를 게스트에 쓰기 가능 상태로 연결하면 데이터가 손상될 수 있습니다. 별도 가상 디스크를 우선 사용하세요.
- **설치 방법은 환경에 따라 달라집니다.** QEMU 상류 공식 문서는 모든 Termux 패키지 이름이나 Android 기기별 설정을 보증하지 않습니다.

## 공식 출처

- QEMU 프로젝트: https://www.qemu.org/
- QEMU 최신 문서: https://www.qemu.org/docs/master/
- Termux 패키지 저장소: https://github.com/termux/termux-packages/
