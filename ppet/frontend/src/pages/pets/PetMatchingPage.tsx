import { useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import {
  FiActivity, FiArrowLeft, FiArrowRight, FiArrowUpRight, FiCheck, FiClock,
  FiEdit2, FiHome, FiMinus, FiPlus, FiUsers, FiZap,
} from 'react-icons/fi';
import { MdApartment, MdPets } from 'react-icons/md';
import apiClient from '../../api/client';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import type { PublicPetDTO } from '../../services/petManagementService';

interface Match {
  pet: PublicPetDTO;
  score: number;
  explanation: string;
  fallback: boolean;
  unmetCriteria: string[];
}

const questions = [
  {
    key: 'home',
    category: 'Không gian sống',
    title: 'Bạn sống ở đâu?',
    hint: 'Chọn nơi thú cưng sẽ sinh sống cùng bạn.',
    options: [
      { value: 'APARTMENT', label: 'Căn hộ', detail: 'Căn hộ hoặc chung cư', icon: MdApartment },
      { value: 'HOUSE', label: 'Nhà riêng', detail: 'Nhà phố hoặc nhà có sân', icon: FiHome },
    ],
  },
  {
    key: 'children',
    category: 'Gia đình',
    title: 'Nhà bạn có trẻ nhỏ không?',
    hint: 'Câu trả lời giúp cân nhắc tính cách của thú cưng khi sống cùng gia đình.',
    options: [
      { value: 'yes', label: 'Có trẻ nhỏ', icon: FiUsers },
      { value: 'no', label: 'Không có trẻ nhỏ', icon: FiHome },
    ],
  },
  {
    key: 'activity',
    category: 'Mức vận động',
    title: 'Bạn thường vận động ở mức nào?',
    hint: 'Chọn phương án gần nhất với nhịp sinh hoạt của bạn.',
    options: [
      { value: 'LOW', label: 'Thấp', detail: 'Thích sinh hoạt nhẹ nhàng tại nhà', icon: FiHome },
      { value: 'MEDIUM', label: 'Vừa', detail: 'Thường đi bộ hoặc chơi ngoài trời', icon: FiActivity },
      { value: 'HIGH', label: 'Cao', detail: 'Thường chạy bộ hoặc tập thể thao', icon: FiZap },
    ],
  },
] as const;

const stepLabels = [...questions.map(question => question.category), 'Thời gian chăm sóc'];
const lastStep = stepLabels.length - 1;
const heroImage = `${import.meta.env.BASE_URL}images/pet-matching-hero.webp`;
const scoreFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });

function MatchCard({ match, featured = false }: { match: Match; featured?: boolean }) {
  const { pet, score, explanation } = match;
  const [brokenImage, setBrokenImage] = useState(false);
  const taxonomy = [pet.speciesName, pet.breedName].filter(Boolean).join(' · ');
  return (
    <article className={`min-w-0 overflow-hidden rounded-3xl border bg-white shadow-soft ${featured
      ? 'border-primary-200 md:grid md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]'
      : 'flex h-full flex-col border-stone-200'}`}>
      <div className={`relative overflow-hidden bg-primary-50 ${featured ? 'aspect-[4/3] md:aspect-auto md:min-h-80' : 'aspect-[16/10]'}`}>
        {pet.imageUrl && !brokenImage ? (
          <img
            src={pet.imageUrl}
            alt={pet.name}
            loading={featured ? 'eager' : 'lazy'}
            onError={() => setBrokenImage(true)}
            className="absolute inset-0 size-full object-cover"
          />
        ) : (
          <div className="absolute inset-0 grid place-items-center text-primary-700" role="img" aria-label="Chưa có ảnh">
            <MdPets className="size-12" aria-hidden="true" />
          </div>
        )}
      </div>
      <div className={`flex min-w-0 flex-col items-start ${featured ? 'gap-4 p-5 sm:p-8 lg:p-10' : 'grow gap-3 p-5'}`}>
        <p className="inline-flex items-center gap-2 rounded-full bg-secondary-100 px-3 py-1.5 text-sm font-semibold text-amber-950">
          <MdPets aria-hidden="true" /> {scoreFormat.format(score)}/100 điểm đối chiếu
        </p>
        {match.fallback && (
          <div className="w-full rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950">
            <p className="font-semibold">Gần phù hợp · Cần cân nhắc</p>
            <ul className="mt-2 list-disc space-y-1 pl-5">
              {match.unmetCriteria.map(criterion => <li key={criterion}>{criterion}</li>)}
            </ul>
          </div>
        )}
        <div className="min-w-0">
          <h3 className={`break-words font-bold text-primary-900 ${featured ? 'text-2xl sm:text-4xl' : 'text-xl'}`}>{pet.name}</h3>
          {taxonomy && <p className="mt-1 break-words text-text-secondary">{taxonomy}</p>}
        </div>
        {explanation.trim() && (
          <div className={`min-w-0 break-words text-sm leading-relaxed ${featured ? 'rounded-2xl bg-primary-50 p-4' : 'text-text-secondary'}`}>
            {featured && <p className="mb-1 font-semibold text-primary-900">Vì sao bé được gợi ý?</p>}
            <p className="whitespace-pre-line">{explanation.trim()}</p>
          </div>
        )}
        <Link
          to={`/pets/${pet.id}`}
          className={`inline-flex min-h-11 items-center gap-2 rounded-full font-semibold focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary-700 ${featured
            ? 'bg-primary-700 px-5 text-white hover:bg-primary-800'
            : 'mt-auto text-primary-800 hover:underline'}`}
        >
          Xem chi tiết<span className="sr-only"> {pet.name}</span><FiArrowUpRight aria-hidden="true" />
        </Link>
      </div>
    </article>
  );
}

export const PetMatchingPage = () => {
  const [step, setStep] = useState(0);
  const [answers, setAnswers] = useState({ home: '', children: '', activity: '' });
  const [hours, setHours] = useState('');
  const [results, setResults] = useState<Match[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const heading = useRef<HTMLHeadingElement>(null);
  const request = useRef<AbortController | null>(null);
  const view = results !== null ? 'results' : step;
  const previousView = useRef<typeof view>(view);
  const questionId = useId();
  const progressId = useId();
  const hoursHintId = useId();
  const errorId = useId();
  const question = questions[step];
  const careTime = Number(hours);
  const validHours = hours.trim() !== '' && Number.isInteger(careTime) && careTime >= 0 && careTime <= 24;
  const validAnswer = question ? answers[question.key] !== '' : validHours;

  useEffect(() => {
    if (previousView.current !== view) {
      previousView.current = view;
      heading.current?.focus();
    }
  }, [view]);
  useEffect(() => () => request.current?.abort(), []);

  const changeStep = (next: number) => {
    setError(false);
    setStep(next);
  };

  const adjustHours = (delta: number) => {
    const current = Number.isFinite(careTime) ? Math.trunc(careTime) : 0;
    setError(false);
    setHours(String(Math.max(0, Math.min(24, current + delta))));
  };

  const goForward = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validAnswer || request.current) return;
    if (step < lastStep) {
      changeStep(step + 1);
      return;
    }
    if (!answers.home || !answers.children || !answers.activity) return;

    const controller = new AbortController();
    request.current = controller;
    setLoading(true);
    setError(false);
    try {
      const { data } = await apiClient.post<Match[]>('/pet-matching/submit', {
        livingEnvironment: answers.home,
        houseSize: answers.home,
        presenceOfChildren: answers.children === 'yes',
        activityLevel: answers.activity,
        availableCareTime: careTime,
      }, { signal: controller.signal });
      if (!controller.signal.aborted) setResults(data);
    } catch {
      if (!controller.signal.aborted) setError(true);
    } finally {
      request.current = null;
      if (!controller.signal.aborted) setLoading(false);
    }
  };

  if (results !== null) {
    const [featured, ...more] = results;
    return (
      <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-12">
        <div className="mb-7 flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
          <div>
            <p className="mb-2 text-sm font-semibold text-primary-800">Tìm thú cưng phù hợp</p>
            <h1 ref={heading} tabIndex={-1} className="scroll-mt-28 text-3xl font-bold tracking-tight text-primary-900 outline-none sm:text-4xl">
              {featured ? featured.fallback ? 'Các bé gần phù hợp nhất' : 'Gợi ý dành cho bạn' : 'Chưa đủ dữ liệu để gợi ý'}
            </h1>
          </div>
          <Button variant="outline" size="sm" type="button" className="min-h-11" leftIcon={<FiEdit2 aria-hidden="true" />} onClick={() => {
            setResults(null);
            changeStep(0);
          }}>
            Chỉnh câu trả lời
          </Button>
        </div>
        {featured ? (
          <>
            {featured.fallback && <p role="status" className="mb-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-950">Chưa có bé nào đáp ứng đầy đủ điều kiện của bạn. Dưới đây là các bé gần phù hợp nhất theo điểm đối chiếu; hãy xem kỹ những điều kiện chưa đáp ứng trước khi quyết định.</p>}
            <p className="mb-5 text-sm leading-relaxed text-text-secondary">Điểm đối chiếu dựa trên hồ sơ từng bé do cửa hàng nhập, không phải xác suất phù hợp. Không gian 20 điểm, gia đình 20 điểm; chăm sóc tối đa 30 điểm theo tỷ lệ số giờ bạn có so với nhu cầu của bé, làm tròn xuống; vận động trùng mức 30 điểm, lệch một mức 15 điểm, lệch hai mức 0 điểm. Chỉ khi không có bé đáp ứng đầy đủ, hệ thống mới gợi ý các bé gần phù hợp nhất. Bé thiếu hồ sơ đánh giá sẽ không được chấm điểm.</p>
            <section aria-labelledby="matching-top-title">
              <h2 id="matching-top-title" className="mb-3 text-lg font-semibold">{featured.fallback ? 'Gần phù hợp nhất theo hồ sơ' : 'Gợi ý hàng đầu'}</h2>
              <MatchCard key={featured.pet.id} match={featured} featured />
            </section>
            {more.length > 0 && (
              <section className="mt-9" aria-labelledby="matching-more-title">
                <h2 id="matching-more-title" className="mb-4 text-xl font-bold">Các bé khác bạn có thể cân nhắc</h2>
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {more.map(match => <MatchCard key={match.pet.id} match={match} />)}
                </div>
              </section>
            )}
          </>
        ) : (
          <div className="rounded-3xl border border-primary-100 bg-white p-6 sm:p-9">
            <MdPets className="mb-3 size-8 text-primary-700" aria-hidden="true" />
            <p className="mb-4 text-text-secondary">Chưa có bé đang bán, chưa được giữ chỗ và có đủ hồ sơ đánh giá để đối chiếu. Hệ thống không tự suy đoán thông tin còn thiếu. Bạn có thể quay lại sau hoặc xem danh sách thú cưng.</p>
            <Link to="/pets" className="inline-flex min-h-11 items-center gap-2 font-semibold text-primary-800 underline underline-offset-4">
              Xem danh sách thú cưng <FiArrowRight aria-hidden="true" />
            </Link>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="mx-auto grid max-w-6xl gap-4 px-4 py-5 sm:gap-6 sm:px-6 sm:py-10 lg:grid-cols-[minmax(0,0.83fr)_minmax(0,1.17fr)]">
      <section className="relative isolate flex min-h-52 min-w-0 items-end overflow-hidden rounded-3xl bg-primary-900 p-5 text-white sm:min-h-64 sm:p-8 lg:min-h-[35rem]">
        <img src={heroImage} width={880} height={1100} alt="" className="absolute inset-0 -z-20 size-full object-cover object-[center_38%]" />
        <div className="absolute inset-0 -z-10 bg-linear-to-t from-[#032d24] via-[#032d24]/35 to-transparent" aria-hidden="true" />
        <div className="relative max-w-sm">
          <h1 className="text-2xl leading-tight font-bold tracking-tight text-balance text-white sm:text-4xl lg:text-[2.65rem]">Tìm thú cưng phù hợp</h1>
          <p className="mt-2 max-w-xs text-sm leading-relaxed text-white sm:text-base">Trả lời 4 câu hỏi để đối chiếu lối sống của bạn với hồ sơ đã được cửa hàng đánh giá cho từng bé.</p>
        </div>
      </section>
      <section className="flex min-w-0 flex-col rounded-3xl border border-stone-200 bg-white p-5 shadow-soft sm:p-8 lg:p-10">
        <div className="mb-6 sm:mb-8">
          <p id={progressId} className="mb-3 flex flex-wrap justify-between gap-x-4 gap-y-1 text-sm font-semibold text-primary-800">
            <span>Bước {step + 1}/{stepLabels.length}</span>
            <span>{stepLabels[step]}</span>
          </p>
          <div
            role="progressbar"
            aria-labelledby={progressId}
            aria-valuemin={1}
            aria-valuemax={stepLabels.length}
            aria-valuenow={step + 1}
            className="grid grid-cols-4 gap-2"
          >
            {stepLabels.map((label, index) => (
              <span key={label} className={`h-1.5 rounded-full ${index < step ? 'bg-primary-400' : index === step ? 'bg-primary-700' : 'bg-primary-100'}`} />
            ))}
          </div>
        </div>

        <form noValidate onSubmit={goForward} className="flex flex-1 flex-col" aria-busy={loading}>
          <fieldset className="min-w-0" disabled={loading} aria-labelledby={questionId}>
            <h2 id={questionId} ref={heading} tabIndex={-1} className="scroll-mt-28 text-2xl leading-tight font-bold tracking-tight text-primary-900 outline-none sm:text-3xl">
              {question?.title ?? 'Bạn có bao nhiêu giờ chăm sóc mỗi ngày?'}
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-text-secondary">
              {question?.hint ?? 'Tính cả thời gian cho ăn, đi dạo và chơi cùng thú cưng.'}
            </p>
            {question ? (
              <div className="mt-5 grid gap-2.5">
                {question.options.map(option => {
                  const selected = answers[question.key] === option.value;
                  return (
                    <label key={option.value} className="relative min-w-0 cursor-pointer">
                      <input
                        className="peer sr-only"
                        type="radio"
                        name={question.key}
                        value={option.value}
                        checked={selected}
                        onChange={() => setAnswers(current => ({ ...current, [question.key]: option.value }))}
                      />
                      <span className={`flex min-h-16 items-center gap-3.5 rounded-2xl border px-3 py-2.5 transition-colors peer-focus-visible:ring-2 peer-focus-visible:ring-primary-700 peer-focus-visible:ring-offset-2 sm:px-4 ${selected
                        ? 'border-primary-700 bg-primary-50'
                        : 'border-stone-200 hover:border-primary-400 hover:bg-primary-50/50'}`}>
                        <span className={`grid size-10 shrink-0 place-items-center rounded-xl ${selected ? 'bg-primary-700 text-white' : 'bg-primary-50 text-primary-800'}`}>
                          <option.icon size={20} aria-hidden="true" />
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block font-semibold text-primary-900">{option.label}</span>
                          {'detail' in option && <span className="mt-0.5 block text-sm text-text-secondary">{option.detail}</span>}
                        </span>
                        <span aria-hidden="true" className={`grid size-5 shrink-0 place-items-center rounded-full border ${selected ? 'border-primary-700 bg-primary-700 text-white' : 'border-stone-400'}`}>
                          {selected && <FiCheck size={13} />}
                        </span>
                      </span>
                    </label>
                  );
                })}
              </div>
            ) : (
              <div className="mt-5 rounded-2xl border border-primary-100 bg-primary-50/65 p-4 sm:p-5">
                <div className="flex max-w-xs items-start gap-2">
                  <Button type="button" variant="outline" className="size-11 shrink-0 !p-0" aria-label="Bớt một giờ" disabled={hours.trim() === '' || careTime <= 0} onClick={() => adjustHours(-1)}>
                    <FiMinus aria-hidden="true" />
                  </Button>
                  <Input
                    type="number"
                    min={0}
                    max={24}
                    step={1}
                    required
                    aria-label="Số giờ chăm sóc mỗi ngày"
                    aria-describedby={hoursHintId}
                    placeholder="Nhập số giờ"
                    value={hours}
                    error={hours !== '' && !validHours ? 'Nhập số nguyên từ 0 đến 24.' : undefined}
                    onChange={event => {
                      setError(false);
                      setHours(event.target.value);
                    }}
                    className="min-h-11 bg-white text-center [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
                  />
                  <Button type="button" variant="outline" className="size-11 shrink-0 !p-0" aria-label="Thêm một giờ" disabled={careTime >= 24} onClick={() => adjustHours(1)}>
                    <FiPlus aria-hidden="true" />
                  </Button>
                </div>
                <p id={hoursHintId} className="mt-3 flex items-center gap-2 text-sm text-text-secondary">
                  <FiClock aria-hidden="true" /> 0–24 giờ mỗi ngày
                </p>
              </div>
            )}
          </fieldset>

          {error && <p id={errorId} role="alert" className="mt-4 rounded-xl bg-red-50 p-3 text-sm text-red-800">Không tải được kết quả. Câu trả lời của bạn vẫn được giữ.</p>}
          {loading && <p role="status" className="sr-only">Đang tìm thú cưng phù hợp…</p>}
          <div className="mt-auto flex flex-wrap items-center justify-between gap-2 pt-6">
            {step > 0 && (
              <Button type="button" variant="secondary" size="sm" className="min-h-11" disabled={loading} leftIcon={<FiArrowLeft aria-hidden="true" />} onClick={() => changeStep(step - 1)}>
                Quay lại
              </Button>
            )}
            <Button
              type="submit"
              className="ml-auto min-h-11 !bg-primary-700 !text-white hover:!bg-primary-800 max-[375px]:grow"
              isLoading={loading}
              disabled={!validAnswer}
              aria-describedby={error ? errorId : undefined}
              rightIcon={<FiArrowRight aria-hidden="true" />}
            >
              {loading ? 'Đang tìm thú cưng phù hợp…' : error ? 'Thử lại' : step === lastStep ? 'Xem gợi ý' : 'Tiếp tục'}
            </Button>
          </div>
        </form>
      </section>
    </div>
  );
};
