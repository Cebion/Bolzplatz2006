#include <sys/stat.h>

/* bind stat() to the pre-2.33 __xstat ABI so the lib runs on glibc 2.29 CFWs */
extern int xstat_compat(int ver, const char *path, struct stat *buf);
__asm__(".symver xstat_compat,__xstat@GLIBC_2.17");

int __wrap_stat(const char *path, struct stat *buf)
{
  return xstat_compat(0, path, buf);
}
