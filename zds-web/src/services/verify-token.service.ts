import UserInfoRemoteService from '@/services/remote/UserInfo';
const remoteService = new UserInfoRemoteService();
const TIME = 1000 * 60;
export function verifyTokenInterval() {
  let success = true;
  const intervalId = setInterval(() => {
    if (success) {
      remoteService.verifyToken().catch(() => {
        success = false;
        clearInterval(intervalId);
      });
    } else {
      clearInterval(intervalId);
    }
  }, TIME);
}
