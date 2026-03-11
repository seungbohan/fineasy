'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import { useAuthStore } from '@/stores/auth-store';
import { apiClient } from '@/lib/api-client';

interface AuthApiResponse {
  accessToken: string;
  refreshToken: string;
  user: { id: number; email: string; nickname: string };
}

export default function SignupPage() {
  const router = useRouter();
  const { login } = useAuthStore();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!email.trim()) {
      newErrors.email = '이메일을 입력해주세요.';
    } else if (!email.includes('@')) {
      newErrors.email = '올바른 이메일 형식을 입력해주세요.';
    }

    if (!nickname.trim()) {
      newErrors.nickname = '닉네임을 입력해주세요.';
    } else if (nickname.length < 2 || nickname.length > 20) {
      newErrors.nickname = '닉네임은 2~20자 사이로 입력해주세요.';
    }

    if (!password) {
      newErrors.password = '비밀번호를 입력해주세요.';
    } else if (password.length < 8) {
      newErrors.password = '비밀번호는 8자 이상이어야 합니다.';
    }

    if (password !== confirmPassword) {
      newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);

    try {
      const data = await apiClient.post<AuthApiResponse>('/auth/signup', { email, password, nickname });
      login(
        { id: data.user.id, email: data.user.email, nickname: data.user.nickname, createdAt: new Date().toISOString() },
        data.accessToken,
        data.refreshToken,
      );
      router.push('/');
    } catch (err) {
      setErrors({ form: err instanceof Error ? err.message : '회원가입에 실패했습니다.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-56px-64px)] items-center justify-center px-4 py-8">
      <Card className="w-full max-w-md rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-6 sm:p-8">
          <div className="mb-6 text-center">
            <h1 className="text-xl font-bold text-gray-900">회원가입</h1>
            <p className="mt-1 text-sm text-gray-500">
              FinEasy와 함께 투자를 시작하세요
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label
                htmlFor="email"
                className="mb-1.5 block text-sm font-medium text-gray-700"
              >
                이메일
              </label>
              <Input
                id="email"
                type="email"
                placeholder="example@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                className="h-11"
                aria-invalid={!!errors.email}
              />
              {errors.email && (
                <p className="mt-1 text-xs text-red-500">{errors.email}</p>
              )}
            </div>

            <div>
              <label
                htmlFor="nickname"
                className="mb-1.5 block text-sm font-medium text-gray-700"
              >
                닉네임
              </label>
              <Input
                id="nickname"
                type="text"
                placeholder="투자초보"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                className="h-11"
                aria-invalid={!!errors.nickname}
              />
              {errors.nickname && (
                <p className="mt-1 text-xs text-red-500">{errors.nickname}</p>
              )}
            </div>

            <div>
              <label
                htmlFor="password"
                className="mb-1.5 block text-sm font-medium text-gray-700"
              >
                비밀번호
              </label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="8자 이상 입력"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="new-password"
                  className="h-11 pr-10"
                  aria-invalid={!!errors.password}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1 text-xs text-red-500">{errors.password}</p>
              )}
            </div>

            <div>
              <label
                htmlFor="confirmPassword"
                className="mb-1.5 block text-sm font-medium text-gray-700"
              >
                비밀번호 확인
              </label>
              <Input
                id="confirmPassword"
                type={showPassword ? 'text' : 'password'}
                placeholder="비밀번호 재입력"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                autoComplete="new-password"
                className="h-11"
                aria-invalid={!!errors.confirmPassword}
              />
              {errors.confirmPassword && (
                <p className="mt-1 text-xs text-red-500">
                  {errors.confirmPassword}
                </p>
              )}
            </div>

            {errors.form && (
              <p className="text-sm text-red-500" role="alert">{errors.form}</p>
            )}

            <Button
              type="submit"
              className="h-11 w-full bg-[#3182F6] text-white hover:bg-[#2270E0]"
              disabled={loading}
            >
              {loading ? '가입 중...' : '가입하기'}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              이미 계정이 있으신가요?{' '}
              <Link
                href="/login"
                className="font-medium text-[#3182F6] hover:underline"
              >
                로그인
              </Link>
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
