package dev.usbharu.stl.plugins
/**
 * 要求されたリソースが見つからなかったことを示すカスタム例外。
 * これをスローすると、StatusPagesプラグインが404 Not Foundページを表示します。
 */
class NotFoundException(message: String = "Resource not found") : RuntimeException(message)
