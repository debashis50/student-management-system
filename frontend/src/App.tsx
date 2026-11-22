import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from 'react'
import { api, Course, Dashboard, Enrollment, money, token, User } from './api'

type LoginResult = { token: string; tokenType: string; userId: number; username: string; fullName: string; role: User['role'] }
type CourseForm = {
  courseCode: string; title: string; category: string; description: string; syllabus: string
  fee: string; durationWeeks: string; creditHours: string; startDate: string; endDate: string
  enrollmentDeadline: string; capacity: string; teacherId: string
}
const emptyForm: CourseForm = {
  courseCode: '', title: '', category: '', description: '', syllabus: '', fee: '0',
  durationWeeks: '4', creditHours: '0', startDate: '', endDate: '', enrollmentDeadline: '',
  capacity: '0', teacherId: '',
}

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [loginName, setLoginName] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [isRegistering, setIsRegistering] = useState(false)
  const [loginBusy, setLoginBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [tab, setTab] = useState('catalog')
  const [courses, setCourses] = useState<Course[]>([])
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [summary, setSummary] = useState({ activeCourseCount: 0, totalAmount: 0 })
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('all')
  const [form, setForm] = useState<CourseForm>(emptyForm)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [academicRows, setAcademicRows] = useState<any[]>([])
  const [profile, setProfile] = useState({ fullName: '', email: '' })
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '' })
  const [adminUsers, setAdminUsers] = useState<any[]>([])
  const [adminUserQuery, setAdminUserQuery] = useState('')
  const [adminRoleFilter, setAdminRoleFilter] = useState('ALL')
  const [adminStatusFilter, setAdminStatusFilter] = useState('ALL')
  const [adminEnrollments, setAdminEnrollments] = useState<any[]>([])
  const [courseStudents, setCourseStudents] = useState<any[]>([])
  const [userForm, setUserForm] = useState({ fullName: '', email: '', password: '', role: 'TEACHER' })
  const [academicCourseId, setAcademicCourseId] = useState('')
  const [academicForm, setAcademicForm] = useState({ studentId: '', date: new Date().toISOString().slice(0, 10), present: true, note: '', assessmentName: '', score: '', maxScore: '100', remarks: '', title: '', description: '', dueDate: '', message: '' })

  const loadCourses = useCallback(async () => {
    const data = await api<Course[]>('/courses')
    setCourses(data)
  }, [])

  const loadStudentData = useCallback(async () => {
    const [enrolled, totals] = await Promise.all([
      api<Enrollment[]>('/enrollments/my'),
      api<{ activeCourseCount: number; totalAmount: number }>('/enrollments/my/summary'),
    ])
    setEnrollments(enrolled)
    setSummary(totals)
  }, [])

  const loadDashboard = useCallback(async () => {
    setDashboard(await api<Dashboard>('/admin/dashboard'))
  }, [])

  const refresh = useCallback(async (role?: User['role']) => {
    await loadCourses()
    if (role === 'STUDENT') await loadStudentData()
    if (role === 'ADMIN') await loadDashboard()
  }, [loadCourses, loadStudentData, loadDashboard])

  useEffect(() => {
    const existingToken = token()
    if (!existingToken) return
    api<User>('/auth/me').then(async current => {
      setUser(current)
      setTab(current.role === 'ADMIN' ? 'dashboard' : current.role === 'TEACHER' ? 'teaching' : 'catalog')
      await refresh(current.role)
    }).catch(() => {
      localStorage.removeItem('sms_token')
      setUser(null)
    })
  }, [refresh])

  useEffect(() => {
    if (!user) return
    const pathByTab: Record<string, string> = {
      attendance: '/student/attendance', results: '/student/results',
      assignments: '/student/assignments', notices: '/student/notices',
    }
    if (pathByTab[tab]) api<any[]>(pathByTab[tab]).then(setAcademicRows).catch(e => setError(e.message))
    if (['users', 'students', 'inactive', 'teachers'].includes(tab) && user.role === 'ADMIN') api<any[]>('/admin/users').then(setAdminUsers).catch(e => setError(e.message))
    if (tab === 'active-enrollments' && user.role === 'ADMIN') api<any[]>('/admin/enrollments/active').then(setAdminEnrollments).catch(e => setError(e.message))
    if (tab === 'profile') api<User>('/profile/me').then(u => setProfile({ fullName: u.fullName, email: u.email })).catch(e => setError(e.message))
  }, [tab, user])

  useEffect(() => {
    if (!user || !academicCourseId || user.role !== 'TEACHER') { setCourseStudents([]); return }
    api<any[]>(`/teacher/courses/${academicCourseId}/students`).then(setCourseStudents).catch(e => setError(e.message))
  }, [academicCourseId, user])

  async function saveProfile(e: FormEvent) {
    e.preventDefault()
    try { const updated = await api<User>('/profile/me', { method: 'PUT', body: JSON.stringify(profile) }); setUser(updated); setNotice('Profile updated successfully.') }
    catch (e) { setError(e instanceof Error ? e.message : 'Profile update failed') }
  }

  async function changePassword(e: FormEvent) {
    e.preventDefault()
    try {
      await api('/auth/password', { method: 'PUT', body: JSON.stringify(passwordForm) })
      setPasswordForm({ currentPassword: '', newPassword: '' })
      setNotice('Password changed successfully.')
    } catch (e) { setError(e instanceof Error ? e.message : 'Password change failed') }
  }

  async function createManagedUser(e: FormEvent) {
    e.preventDefault()
    try {
      await api('/admin/users', { method: 'POST', body: JSON.stringify(userForm) })
      setUserForm({ fullName: '', email: '', password: '', role: 'TEACHER' })
      setAdminUsers(await api<any[]>('/admin/users'))
      setNotice('Account created successfully.')
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not create account') }
  }

  async function deleteManagedUser(id: number, fullName: string) {
    const confirmed = window.confirm(
        `Permanently delete ${fullName}?\n\n` +
        `This cannot be undone. If academic or teaching records exist, ` +
        `the backend will block deletion.`
    );

    if (!confirmed) return;

    setError('');
    setNotice('');

    try {
      await api(`/admin/users/${id}/permanent`, {
        method: 'DELETE',
      });

      // Refresh the user directory and dashboard counts.
      const [updatedUsers] = await Promise.all([
        api<any[]>('/admin/users'),
        loadDashboard(),
        loadCourses(),
      ]);

      setAdminUsers(updatedUsers);
      setNotice(`${fullName} was permanently deleted.`);
    } catch (e) {
      setError(
          e instanceof Error
              ? e.message
              : 'Could not permanently delete this user.'
      );
    }
  }

  async function saveAcademic(courseId: string, endpoint: string, payload: unknown) {
    try {
      await api(`/teacher/courses/${courseId}/${endpoint}`, { method: 'POST', body: JSON.stringify(payload) })
      setNotice('Saved successfully.')
      setAcademicForm(f => ({ ...f, studentId: '', assessmentName: '', score: '', title: '', description: '', message: '' }))
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not save') }
  }

  async function doLogin(e: FormEvent) {
    e.preventDefault()
    setError('')
    setNotice('')
    setLoginBusy(true)
    try {
      if (isRegistering) {
        await api<{ message: string }>('/auth/register', {
          method: 'POST',
          body: JSON.stringify({ fullName, email: loginName, password }),
        })
        setIsRegistering(false)
        setPassword('')
        setNotice('Registration successful. Please sign in with your email and password.')
      } else {
        const result = await api<LoginResult>('/auth/login', {
          method: 'POST',
          body: JSON.stringify({ username: loginName, password }),
        })
        localStorage.setItem('sms_token', result.token)
        const current: User = await api<User>('/auth/me')
        setUser(current)
        setTab(current.role === 'ADMIN' ? 'dashboard' : current.role === 'TEACHER' ? 'teaching' : 'catalog')
        await refresh(current.role)
        setNotice(`Welcome, ${current.fullName}!`)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : (isRegistering ? 'Registration failed' : 'Login failed'))
    } finally {
      setLoginBusy(false)
    }
  }

  function logout() {
    localStorage.removeItem('sms_token')
    setUser(null)
    setCourses([])
    setEnrollments([])
    setDashboard(null)
    setNotice('')
    setError('')
  }

  async function doEnroll(course: Course) {
    setError(''); setNotice('')
    try {
      await api<Enrollment>(`/enrollments/${course.id}`, { method: 'POST' })
      await loadStudentData()
      setNotice(`Enrolled in ${course.title}. No payment was made; the fee is only shown as a total.`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Enrollment failed')
    }
  }

  async function doDrop(enrollment: Enrollment) {
    if (!window.confirm(`Drop "${enrollment.courseTitle}"? Its fee will be removed from your active total.`)) return
    setError(''); setNotice('')
    try {
      await api(`/enrollments/${enrollment.courseId}`, { method: 'DELETE' })
      await loadStudentData()
      setNotice(`${enrollment.courseTitle} dropped. Total amount recalculated.`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not drop course')
    }
  }

  function startEdit(course: Course) {
    setEditingId(course.id)
    setForm({
      courseCode: course.courseCode, title: course.title, category: course.category || '',
      description: course.description || '', syllabus: course.syllabus || '', fee: String(course.fee),
      durationWeeks: String(course.durationWeeks), creditHours: String(course.creditHours),
      startDate: course.startDate || '', endDate: course.endDate || '',
      enrollmentDeadline: course.enrollmentDeadline || '', capacity: String(course.capacity),
      teacherId: course.teacherId ? String(course.teacherId) : '',
    })
    setTab('manage')
  }

  async function saveCourse(e: FormEvent) {
    e.preventDefault()
    setSaving(true); setError(''); setNotice('')
    const payload = {
      ...form,
      fee: Number(form.fee),
      durationWeeks: Number(form.durationWeeks),
      creditHours: Number(form.creditHours),
      capacity: Number(form.capacity),
      teacherId: form.teacherId ? Number(form.teacherId) : null,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      enrollmentDeadline: form.enrollmentDeadline || null,
    }
    try {
      const path = editingId ? `/courses/${editingId}` : '/courses'
      await api(path, { method: editingId ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      setForm(emptyForm); setEditingId(null)
      await refresh(user?.role)
      setNotice(editingId ? 'Course updated.' : 'Course created as draft. Publish it to show students.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save course')
    } finally {
      setSaving(false)
    }
  }


  async function courseAction(
      course: Course,
      action: 'publish' | 'archive' | 'enrollment' | 'relaunch' | 'delete'
  ) {
    if (
        action === 'delete' &&
        !window.confirm(
            `Permanently delete "${course.title}"? This cannot be undone.`
        )
    ) {
      return
    }

    setError('')
    setNotice('')

    try {
      let path: string
      let method: string
      let body: string | undefined

      switch (action) {
        case 'enrollment':
          path = `/courses/${course.id}/enrollment`
          method = 'PATCH'
          body = JSON.stringify({ open: !course.enrollmentOpen })
          break

        case 'delete':
          path = `/courses/${course.id}/permanent`
          method = 'DELETE'
          break

        case 'relaunch':
          path = `/courses/${course.id}/relaunch`
          method = 'PATCH'
          break

        case 'publish':
        case 'archive':
          path = `/courses/${course.id}/${action}`
          method = 'PATCH'
          break
      }

      await api(path, {
        method,
        ...(body ? { body } : {}),
      })

      await refresh(user?.role)

      setNotice(
          action === 'relaunch'
              ? 'Course restored. Publish it to make it available to students.'
              : action === 'delete'
                  ? 'Course permanently deleted.'
                  : action === 'publish'
                      ? 'Course published.'
                      : action === 'archive'
                          ? 'Course archived.'
                          : 'Enrollment setting updated.'
      )
    } catch (err) {
      setError(
          err instanceof Error ? err.message : 'Course action failed'
      )
    }
  }


  const activeIds = useMemo(() => new Set(enrollments.filter(e => e.status === 'ACTIVE').map(e => e.courseId)), [enrollments])
  const categories = useMemo(() => ['all', ...Array.from(new Set(courses.map(c => c.category).filter((v): v is string => Boolean(v))))], [courses])
  const filteredCourses = courses.filter(c =>
      `${c.title} ${c.courseCode} ${c.category || ''} ${c.teacherName || ''}`.toLowerCase().includes(query.toLowerCase())
      && (category === 'all' || c.category === category),
  )
  const myActive = enrollments.filter(e => e.status === 'ACTIVE')
  const myDropped = enrollments.filter(e => e.status === 'DROPPED')

  if (!user) {
    return <main className="login-shell">
      <section className="login-art">
        <div className="brand-mark">CH</div>
        <p className="eyebrow">ONLINE LEARNING PLATFORM</p>
        <h1>Learn something<br /><em>new every day.</em></h1>
        <p className="muted-light">Discover courses, learn at your pace, and keep all your learning in one place.</p>
        <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" />
      </section>
      <section className="login-panel">
        <div className="login-card">
          <div className="brand-small">CourseHub <span>•</span></div>
          <h2>{isRegistering ? 'Create student account' : 'Welcome back'}</h2>
          <p className="subtle">{isRegistering ? 'Register with your email to start learning.' : 'Sign in to continue to your workspace.'}</p>
          {error && <div className="alert error">{error}</div>}
          {notice && !user && <div className="alert success">{notice}</div>}
          <form onSubmit={doLogin} className="stack-form">
            {isRegistering && <label>Full name<input value={fullName} onChange={e => setFullName(e.target.value)} autoComplete="name" maxLength={120} required /></label>}
            <label>Email address<input type="email" value={loginName} onChange={e => setLoginName(e.target.value)} autoComplete="email" required /></label>
            <label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete={isRegistering ? 'new-password' : 'current-password'} minLength={isRegistering ? 8 : undefined} required /></label>
            {isRegistering && <p className="tiny-note">Use at least 8 characters. Registration creates a student account only.</p>}
            <button className="primary full" disabled={loginBusy}>{loginBusy ? (isRegistering ? 'Creating account…' : 'Signing in…') : (isRegistering ? 'Create account →' : 'Sign in →')}</button>
          </form>
          <p className="subtle auth-switch">{isRegistering ? 'Already have an account?' : 'New student?'} <button type="button" className="text-button" onClick={() => { setIsRegistering(!isRegistering); setError(''); setNotice(''); setPassword('') }}>{isRegistering ? 'Sign in' : 'Register here'}</button></p>
          <p className="tiny-note">Local demo • No actual payments are collected.</p>
        </div>
      </section>
    </main>
  }

  const nav = user.role === 'ADMIN'
      ? [{ id: 'dashboard', label: 'Dashboard', icon: '▦' },
        { id: 'manage', label: 'Course management', icon: '▤' },
        { id: 'users', label: 'User management', icon: '♙' },
        { id: 'catalog', label: 'Course catalog', icon: '◫' },
        { id: 'profile', label: 'My profile', icon: '◉' },
        { id: 'trash', label: 'Trash', icon: '♲' }
      ]
      : user.role === 'TEACHER'
          ? [{ id: 'teaching', label: 'My teaching', icon: '▤' },
            { id: 'catalog', label: 'Course catalog', icon: '◫' },
            { id: 'profile', label: 'My profile', icon: '◉' }
          ]
          : [{ id: 'catalog', label: 'Explore courses', icon: '◫' },
            { id: 'mycourses', label: 'My courses', icon: '▣' },
            { id: 'attendance', label: 'Attendance', icon: '✓' },
            { id: 'results', label: 'Results', icon: '≡' },
            { id: 'assignments', label: 'Assignments', icon: '▤' },
            { id: 'notices', label: 'Notices', icon: '◉' },
            { id: 'profile', label: 'My profile', icon: '♙' }
          ]


  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark small">CH</div><span>CourseHub</span></div>
      <div className="workspace-label">WORKSPACE</div>
      <nav>{nav.map(item => <button key={item.id} className={`nav-item ${tab === item.id ? 'selected' : ''}`} onClick={() => { setTab(item.id); setError(''); setNotice('') }}><span className="nav-icon">{item.icon}</span>{item.label}</button>)}</nav>
      <div className="sidebar-bottom">
        <div className="avatar">{user.fullName.split(' ').map(s => s[0]).slice(0, 2).join('').toUpperCase()}</div>
        <div className="user-mini"><strong>{user.fullName}</strong><span>{user.role.toLowerCase()}</span></div>
        <button className="icon-button" title="Log out" onClick={logout}>↗</button>
      </div>
    </aside>
    <main className="main-panel">
      <header className="topbar">
        <div><span className="breadcrumb">Workspace / </span><strong>{nav.find(n => n.id === tab)?.label || 'Workspace'}</strong></div>
        <div className="top-user"><span className="online-dot" /> {user.fullName} <span className="role-chip">{user.role}</span></div>
      </header>
      <div className="page-content">
        {error && <div className="alert error"><span>{error}</span><button onClick={() => setError('')}>×</button></div>}
        {notice && <div className="alert success"><span>{notice}</span><button onClick={() => setNotice('')}>×</button></div>}

        {tab === 'dashboard' && user.role === 'ADMIN' && <section>
          <div className="page-heading"><div><p className="eyebrow">OVERVIEW</p><h1>Dashboard</h1><p className="subtle">Here is what is happening with your learning platform.</p></div><button className="secondary" onClick={() => refresh(user.role)}>↻ Refresh data</button></div>
          <div className="stat-grid">
            <Stat label="Total students" value={dashboard?.totalStudents ?? '—'} detail={`${dashboard?.activeStudents ?? 0} active · View details →`} icon="♙" onClick={() => { setAdminRoleFilter("STUDENT"); setAdminStatusFilter("ALL"); setTab("students") }} />
            <Stat label="Inactive students" value={dashboard?.inactiveStudents ?? '—'} detail="Accounts disabled · View details →" icon="⊘" onClick={() => { setAdminRoleFilter("STUDENT"); setAdminStatusFilter("INACTIVE"); setTab("inactive") }} />
            <Stat label="Teachers" value={dashboard?.totalTeachers ?? '—'} detail="Instructor accounts · View details →" icon="♧" onClick={() => { setAdminRoleFilter("TEACHER"); setAdminStatusFilter("ALL"); setTab("teachers") }} />
            <Stat label="All courses" value={dashboard?.totalCourses ?? '—'} detail={`${dashboard?.publishedCourses ?? 0} published · View details →`} icon="▤" onClick={() => setTab("manage")} />
            <Stat label="Active enrollments" value={dashboard?.activeEnrollments ?? '—'} detail="Current learners · View details →" icon="↗" onClick={() => setTab("active-enrollments")} />
          </div>
          <div className="two-column">
            <div className="panel-card"><div className="panel-title"><h3>Course status</h3><span className="pill">Live counts</span></div>
              <StatusRow label="Published" value={dashboard?.publishedCourses ?? 0} total={Math.max(dashboard?.totalCourses ?? 0, 1)} />
              <StatusRow label="Draft" value={dashboard?.draftCourses ?? 0} total={Math.max(dashboard?.totalCourses ?? 0, 1)} />
              <StatusRow label="Archived" value={dashboard?.archivedCourses ?? 0} total={Math.max(dashboard?.totalCourses ?? 0, 1)} />
            </div>
            <div className="panel-card highlight-card"><p className="eyebrow">PLATFORM SNAPSHOT</p><h3>Keep learning moving.</h3><p className="subtle">Manage your course catalog, publish new learning opportunities, and monitor enrollment activity from one place.</p><button className="primary" onClick={() => setTab('manage')}>Manage courses →</button></div>
          </div>
        </section>}

        {tab === 'catalog' && <section>
          <div className="page-heading"><div><p className="eyebrow">{user.role === 'ADMIN' ? 'CONTENT' : 'LEARN SOMETHING NEW'}</p><h1>Course catalog</h1><p className="subtle">Explore published courses and see the fee before you enroll.</p></div><div className="course-count">{filteredCourses.length} courses</div></div>
          <div className="filter-bar"><div className="search-box"><span>⌕</span><input placeholder="Search course, code or instructor…" value={query} onChange={e => setQuery(e.target.value)} /></div><select value={category} onChange={e => setCategory(e.target.value)}>{categories.map(c => <option key={c} value={c}>{c === 'all' ? 'All categories' : c}</option>)}</select></div>
          {filteredCourses.length === 0 ? <Empty title="No courses found" text="Try another search or ask the Admin to publish a course." /> :
              <div className="course-grid">{filteredCourses.map((course, index) => <article className="course-card" key={course.id}>
                <div className={`course-cover cover-${index % 4}`}><span className="cover-symbol">{['⌘', '▦', 'λ', '✳'][index % 4]}</span><span className="cover-category">{course.category || 'General'}</span></div>
                <div className="course-body"><div className="course-code">{course.courseCode}</div><h3>{course.title}</h3><p className="course-description">{course.description || 'A practical course to help you build useful skills.'}</p>
                  <div className="course-meta"><span>◷ {course.durationWeeks} weeks</span><span>◇ {course.creditHours} credits</span></div>
                  <div className="course-instructor">Instructor <strong>{course.teacherName || 'Not assigned'}</strong></div>
                  <div className="course-footer"><div><span className="fee-label">Course fee</span><strong className="fee">{money(course.fee)}</strong></div>
                    {user.role === 'STUDENT' && (activeIds.has(course.id) ? <span className="pill enrolled">Enrolled ✓</span> : <button className="primary compact" disabled={course.status !== 'PUBLISHED' || !course.enrollmentOpen || (course.capacity > 0 && course.enrolledCount >= course.capacity)} onClick={() => doEnroll(course)}>Enroll now</button>)}
                    {user.role === 'ADMIN' && <button className="secondary compact" onClick={() => startEdit(course)}>Edit</button>}
                  </div>
                  {user.role === 'STUDENT' && <div className="capacity-line">{course.capacity > 0 ? `${Math.max(0, course.capacity - course.enrolledCount)} seats left` : 'Open capacity'} · {course.enrollmentOpen ? 'Enrollment open' : 'Enrollment closed'}</div>}
                </div>
              </article>)}</div>}
        </section>}

        {tab === 'mycourses' && user.role === 'STUDENT' && <section>
          <div className="page-heading"><div><p className="eyebrow">YOUR LEARNING</p><h1>My courses</h1><p className="subtle">Manage your enrollments. Dropping a course removes its fee from your current total.</p></div></div>
          <div className="summary-banner"><div><span className="summary-label">Current total amount</span><strong>{money(summary.totalAmount)}</strong><span className="summary-footnote">Fee total only — no payment has been made or collected.</span></div><div className="summary-count"><strong>{summary.activeCourseCount}</strong><span>active courses</span></div></div>
          <div className="section-heading"><h2>Active courses</h2><span>{myActive.length} enrolled</span></div>
          {myActive.length === 0 ? <Empty title="No active courses yet" text="Browse the course catalog and enroll in a course to get started." action={<button className="primary" onClick={() => setTab('catalog')}>Explore courses</button>} /> :
              <div className="table-wrap"><table><thead><tr><th>Course</th><th>Enrolled on</th><th>Course fee</th><th>Status</th><th></th></tr></thead><tbody>{myActive.map(e => <tr key={e.enrollmentId}><td><strong>{e.courseTitle}</strong><small>{e.courseCode}</small></td><td>{new Date(e.enrolledAt).toLocaleDateString()}</td><td className="fee-cell">{money(e.fee)}</td><td><span className="pill enrolled">Active</span></td><td><button className="text-button danger-text" onClick={() => doDrop(e)}>Drop course</button></td></tr>)}</tbody></table></div>}
          {myDropped.length > 0 && <><div className="section-heading dropped-heading"><h2>Dropped courses</h2><span>{myDropped.length} dropped</span></div><div className="table-wrap"><table><thead><tr><th>Course</th><th>Dropped on</th><th>Fee (not included in total)</th><th>Status</th></tr></thead><tbody>{myDropped.map(e => <tr key={e.enrollmentId}><td><strong>{e.courseTitle}</strong><small>{e.courseCode}</small></td><td>{e.droppedAt ? new Date(e.droppedAt).toLocaleDateString() : '—'}</td><td>{money(e.fee)}</td><td><span className="pill muted-pill">Dropped</span></td></tr>)}</tbody></table></div></>}
        </section>}

        {tab === 'manage' && user.role === 'ADMIN' && <section>
          <div className="page-heading"><div><p className="eyebrow">ADMINISTRATION</p><h1>Course management</h1><p className="subtle">Create, edit, publish and manage enrollment availability.</p></div><button className="primary" onClick={() => { setEditingId(null); setForm(emptyForm) }}>＋ New course</button></div>
          <div className="manage-layout">
            <div className="panel-card course-list-panel"><div className="panel-title"><h3>All courses</h3><span className="pill">{courses.length} total</span></div>
              {courses.map(c => <div className="manage-course" key={c.id}><div className="manage-course-main"><strong>{c.title}</strong><small>{c.courseCode} · {money(c.fee)}</small><div className="manage-status"><span className={`pill ${c.status === 'PUBLISHED' ? 'enrolled' : 'muted-pill'}`}>{c.status}</span><span className={`pill ${c.enrollmentOpen ? 'open-pill' : 'muted-pill'}`}>{c.enrollmentOpen ? 'Enrollment open' : 'Enrollment closed'}</span></div></div><div className="manage-actions"><button className="text-button" onClick={() => startEdit(c)}>Edit</button>{c.status !== 'PUBLISHED' && c.status !== 'ARCHIVED' && <button className="text-button" onClick={() => courseAction(c, 'publish')}>Publish</button>}{c.status === 'PUBLISHED' && <button className="text-button" onClick={() => courseAction(c, 'enrollment')}>{c.enrollmentOpen ? 'Close enrollment' : 'Open enrollment'}</button>}{c.status !== 'ARCHIVED' && <button className="text-button danger-text" onClick={() => courseAction(c, 'archive')}>Archive</button>}</div></div>)}
            </div>
            <div className="panel-card form-panel"><div className="panel-title"><h3>{editingId ? 'Edit course' : 'Create a course'}</h3>{editingId && <button className="text-button" onClick={() => { setEditingId(null); setForm(emptyForm) }}>Cancel edit</button>}</div>
              <form className="stack-form" onSubmit={saveCourse}>
                <div className="form-two"><label>Course code<input required maxLength={20} value={form.courseCode} onChange={e => setForm({ ...form, courseCode: e.target.value })} placeholder="WEB-101" /></label><label>Course title<input required value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} placeholder="Web Development" /></label></div>
                <div className="form-two"><label>Category<input value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} placeholder="Development" /></label><label>Fee (BDT)<input required type="number" min="0" step="0.01" value={form.fee} onChange={e => setForm({ ...form, fee: e.target.value })} /></label></div>
                <label>Description<textarea rows={2} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></label>
                <label>Syllabus<textarea rows={3} value={form.syllabus} onChange={e => setForm({ ...form, syllabus: e.target.value })} placeholder="Topics covered in the course" /></label>
                <div className="form-three"><label>Duration (weeks)<input type="number" min="1" value={form.durationWeeks} onChange={e => setForm({ ...form, durationWeeks: e.target.value })} /></label><label>Credit hours<input type="number" min="0" value={form.creditHours} onChange={e => setForm({ ...form, creditHours: e.target.value })} /></label><label>Max students (0 = unlimited)<input type="number" min="0" value={form.capacity} onChange={e => setForm({ ...form, capacity: e.target.value })} /></label></div>
                <div className="form-three"><label>Start date<input type="date" value={form.startDate} onChange={e => setForm({ ...form, startDate: e.target.value })} /></label><label>End date<input type="date" value={form.endDate} onChange={e => setForm({ ...form, endDate: e.target.value })} /></label><label>Enrollment deadline<input type="date" value={form.enrollmentDeadline} onChange={e => setForm({ ...form, enrollmentDeadline: e.target.value })} /></label></div>
                <label>Teacher user ID (optional)<input type="number" min="1" value={form.teacherId} onChange={e => setForm({ ...form, teacherId: e.target.value })} placeholder="Use 2 for demo teacher" /></label>
                <button className="primary full" disabled={saving}>{saving ? 'Saving…' : editingId ? 'Save changes' : 'Create draft course'}</button>
                <p className="tiny-note">New courses start as Draft. Publish a course and open enrollment to make it available to students.</p>
              </form>
            </div>
          </div>
        </section>}

        {tab === 'trash' && user.role === 'ADMIN' && (
            <section>
              <div className="page-heading">
                <div>
                  <p className="eyebrow">ADMINISTRATION</p>
                  <h1>Trash</h1>
                  <p className="subtle">
                    Restore archived courses or permanently delete them.
                  </p>
                </div>
                <span className="course-count">
        {courses.filter(c => c.status === 'ARCHIVED').length} archived courses
      </span>
              </div>

              {courses.filter(c => c.status === 'ARCHIVED').length === 0 ? (
                  <Empty
                      title="Trash is empty"
                      text="Archived courses will appear here."
                  />
              ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                      <tr>
                        <th>Course code</th>
                        <th>Course title</th>
                        <th>Instructor</th>
                        <th>Actions</th>
                      </tr>
                      </thead>
                      <tbody>
                      {courses
                          .filter(c => c.status === 'ARCHIVED')
                          .map(course => (
                              <tr key={course.id}>
                                <td>{course.courseCode}</td>
                                <td>{course.title}</td>
                                <td>{course.teacherName || 'Not assigned'}</td>
                                <td>
                                  <div className="manage-actions">
                                    <button
                                        className="primary compact"
                                        onClick={() => courseAction(course, 'relaunch')}
                                    >
                                      Relaunch
                                    </button>

                                    <button
                                        className="secondary compact"
                                        onClick={() => courseAction(course, 'delete')}
                                    >
                                      Delete Permanent
                                    </button>
                                  </div>
                                </td>
                              </tr>
                          ))}
                      </tbody>
                    </table>
                  </div>
              )}
            </section>
        )}


        {['students', 'inactive', 'teachers'].includes(tab) && user.role === 'ADMIN' && <section>
          <div className="page-heading"><div><p className="eyebrow">ADMINISTRATION</p><h1>{tab === 'teachers' ? 'Teacher directory' : tab === 'inactive' ? 'Inactive students' : 'Student directory'}</h1><p className="subtle">Search accounts, filter status, and manage account access.</p></div><button className="secondary" onClick={() => api<any[]>('/admin/users').then(setAdminUsers).catch(e => setError(e.message))}>↻ Refresh</button></div>
          <div className="filter-bar"><div className="search-box"><span>⌕</span><input placeholder="Search name, email or ID…" value={adminUserQuery} onChange={e => setAdminUserQuery(e.target.value)} /></div>
            <select value={adminRoleFilter} onChange={e => setAdminRoleFilter(e.target.value)}><option value="ALL">All roles</option><option value="STUDENT">Students</option><option value="TEACHER">Teachers</option></select>
            <select value={adminStatusFilter} onChange={e => setAdminStatusFilter(e.target.value)}><option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select>
          </div>
          <div className="table-wrap"><table><thead><tr><th>User ID</th><th>Name</th><th>Email</th><th>Role</th><th>Account status</th><th>Assigned courses</th><th>Actions</th></tr></thead><tbody>
          {adminUsers.filter(u => (adminRoleFilter === 'ALL' || u.role === adminRoleFilter) && (adminStatusFilter === 'ALL' || (adminStatusFilter === 'ACTIVE' ? u.active : !u.active)) && (tab !== 'inactive' || (u.role === 'STUDENT' && !u.active)) && (tab !== 'teachers' || u.role === 'TEACHER') && (tab !== 'students' || u.role === 'STUDENT') && `${u.id} ${u.fullName} ${u.email}`.toLowerCase().includes(adminUserQuery.toLowerCase())).map(u => <tr key={u.id}><td>{u.id}</td><td>{u.fullName}</td><td>{u.email}</td><td>{u.role}</td><td><span className={`pill ${u.active ? 'enrolled' : 'muted-pill'}`}>{u.active ? 'Active' : 'Inactive'}</span></td><td>{u.role === 'TEACHER' ? (courses.filter(c => c.teacherId === u.id).map(c => c.courseCode).join(', ') || 'Not assigned') : '—'}</td>
            <td>
              {u.role !== 'ADMIN' && (
                  <>
                    <button
                        className="text-button"
                        onClick={async () => {
                          try {
                            await api(`/admin/users/${u.id}/active?value=${!u.active}`, {
                              method: 'PATCH',
                            });
                            setAdminUsers(await api<any[]>('/admin/users'));
                            setNotice(`Account ${u.active ? 'deactivated' : 'activated'}.`);
                          } catch (e) {
                            setError(e instanceof Error ? e.message : 'Update failed');
                          }
                        }}
                    >
                      {u.active ? 'Deactivate' : 'Activate'}
                    </button>

                    {/* Permanent deletion is guarded by a confirmation and backend dependency checks. */}
                    <button
                        type="button"
                        className="text-button danger-text"
                        style={{ marginLeft: 8 }}
                        onClick={() => deleteManagedUser(u.id, u.fullName)}
                    >
                      Delete permanently
                    </button>
                  </>
              )}
            </td></tr>)}
          </tbody></table></div>
          <p className="tiny-note">Registration date is not currently stored by this project, so it cannot be displayed accurately yet.</p>
        </section>}

        {tab === 'active-enrollments' && user.role === 'ADMIN' && <section>
          <div className="page-heading"><div><p className="eyebrow">ADMINISTRATION</p><h1>Active enrollments</h1><p className="subtle">Currently active student-course registrations.</p></div><button className="secondary" onClick={() => api<any[]>('/admin/enrollments/active').then(setAdminEnrollments).catch(e => setError(e.message))}>↻ Refresh</button></div>
          <div className="filter-bar"><div className="search-box"><span>⌕</span><input placeholder="Search student or course…" value={adminUserQuery} onChange={e => setAdminUserQuery(e.target.value)} /></div></div>
          <div className="table-wrap"><table><thead><tr><th>Student</th><th>Email</th><th>Course</th><th>Course code</th><th>Enrollment date</th><th>Status</th></tr></thead><tbody>{adminEnrollments.filter(e => `${e.studentName} ${e.studentEmail} ${e.courseTitle} ${e.courseCode}`.toLowerCase().includes(adminUserQuery.toLowerCase())).map(e => <tr key={e.enrollmentId}><td>{e.studentName}</td><td>{e.studentEmail}</td><td>{e.courseTitle}</td><td>{e.courseCode}</td><td>{e.enrolledAt ? new Date(e.enrolledAt).toLocaleDateString() : '—'}</td><td><span className="pill enrolled">{e.status}</span></td></tr>)}</tbody></table></div>
        </section>}

        {tab === 'users' && user.role === 'ADMIN' && <section>
          <div className="page-heading"><div><p className="eyebrow">ADMINISTRATION</p><h1>User management</h1><p className="subtle">Create teacher accounts and activate or deactivate student/teacher accounts.</p></div></div>
          <div className="panel-card form-panel"><h3>Create account</h3><form className="stack-form" onSubmit={createManagedUser}>
            <label>Full name<input required value={userForm.fullName} onChange={e=>setUserForm({...userForm,fullName:e.target.value})}/></label>
            <label>Email<input required type="email" value={userForm.email} onChange={e=>setUserForm({...userForm,email:e.target.value})}/></label>
            <label>Temporary password (minimum 8 characters)<input required minLength={8} type="password" value={userForm.password} onChange={e=>setUserForm({...userForm,password:e.target.value})}/></label>
            <label>Role<select value={userForm.role} onChange={e=>setUserForm({...userForm,role:e.target.value})}><option value="TEACHER">Teacher</option><option value="STUDENT">Student</option></select></label>
            <button className="primary">Create account</button>
          </form></div>
          <div className="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Action</th></tr></thead><tbody>{adminUsers.map(u=><tr key={u.id}><td>{u.fullName}</td><td>{u.email}</td><td>{u.role}</td><td>{u.active?'Active':'Inactive'}</td><td>
            {u.role !== 'ADMIN' && (
                <>
                  <button
                      type="button"
                      className="text-button"
                      onClick={async () => {
                        try {
                          await api(
                              `/admin/users/${u.id}/active?value=${!u.active}`,
                              { method: 'PATCH' }
                          );
                          setAdminUsers(await api<any[]>('/admin/users'));
                          setNotice(`Account ${u.active ? 'deactivated' : 'activated'}.`);
                        } catch (e) {
                          setError(e instanceof Error ? e.message : 'Update failed');
                        }
                      }}
                  >
                    {u.active ? 'Deactivate' : 'Activate'}
                  </button>
                  <button
                      type="button"
                      className="text-button danger-text"
                      style={{ marginLeft: 8 }}
                      onClick={() => deleteManagedUser(u.id, u.fullName)}
                  >
                    Delete permanently
                  </button>
                </>
            )}
          </td></tr>)}</tbody></table></div>
        </section>}

        {tab === 'profile' && <section><div className="page-heading"><div><p className="eyebrow">ACCOUNT</p><h1>My profile</h1><p className="subtle">Update your name and email address.</p></div></div>
          <div className="panel-card form-panel"><form className="stack-form" onSubmit={saveProfile}><label>Full name<input required value={profile.fullName} onChange={e=>setProfile({...profile,fullName:e.target.value})}/></label><label>Email address<input required type="email" value={profile.email} onChange={e=>setProfile({...profile,email:e.target.value})}/></label><button className="primary">Save profile</button></form></div>
          <div className="panel-card form-panel" style={{marginTop:20}}><h3>Change password</h3><form className="stack-form" onSubmit={changePassword}><label>Current password<input type="password" required value={passwordForm.currentPassword} onChange={e=>setPasswordForm({...passwordForm,currentPassword:e.target.value})}/></label><label>New password (minimum 8 characters)<input type="password" required minLength={8} value={passwordForm.newPassword} onChange={e=>setPasswordForm({...passwordForm,newPassword:e.target.value})}/></label><button className="primary">Update password</button></form></div>
        </section>}

        {['attendance','results','assignments','notices'].includes(tab) && user.role === 'STUDENT' && <section>
          <div className="page-heading"><div><p className="eyebrow">STUDENT PORTAL</p><h1>{({attendance:'Attendance',results:'Results & grades',assignments:'Assignments',notices:'Course notices'} as Record<string,string>)[tab]}</h1><p className="subtle">Records are shown for your active course enrollments.</p></div><button className="secondary" onClick={()=>api<any[]>('/student/'+tab).then(setAcademicRows).catch(e=>setError(e.message))}>Refresh</button></div>
          {academicRows.length===0?<Empty title="Nothing to show yet" text="Your teacher has not published any records for your enrolled courses."/>:
              <div className="table-wrap"><table><thead><tr>{(tab==='attendance'?['Course','Date','Status','Note']:tab==='results'?['Course','Assessment','Score','Remarks']:tab==='assignments'?['Course','Assignment','Due date','Description']:['Course','Notice','Date','Message']).map(h=><th key={h}>{h}</th>)}</tr></thead><tbody>{academicRows.map((r,i)=><tr key={r.id??i}>{tab==='attendance'?<><td>{r.courseTitle}</td><td>{r.date}</td><td>{r.present?'Present':'Absent'}</td><td>{r.note||'—'}</td></>:tab==='results'?<><td>{r.courseTitle}</td><td>{r.assessmentName}</td><td>{r.score} / {r.maxScore}</td><td>{r.remarks||'—'}</td></>:tab==='assignments'?<><td>{r.courseTitle}</td><td>{r.title}</td><td>{r.dueDate||'—'}</td><td>{r.description||'—'}</td></>:<><td>{r.courseTitle}</td><td>{r.title}</td><td>{r.createdAt?new Date(r.createdAt).toLocaleDateString():'—'}</td><td>{r.message}</td></>}</tr>)}</tbody></table></div>}
        </section>}

        {tab === 'teaching' && user.role === 'TEACHER' && <section>
          <div className="page-heading"><div><p className="eyebrow">INSTRUCTOR WORKSPACE</p><h1>My teaching</h1><p className="subtle">Courses assigned to your teacher account.</p></div><button className="secondary" onClick={() => refresh(user.role)}>↻ Refresh</button></div>
          {courses.filter(c => c.teacherId === user.id).length === 0 ? <Empty title="No courses assigned" text="Ask the Admin to assign your account to a course." /> :
              <div className="course-grid">{courses.filter(c => c.teacherId === user.id).map((c, i) => <article className="course-card" key={c.id}><div className={`course-cover cover-${i % 4}`}><span className="cover-symbol">▤</span><span className="cover-category">{c.courseCode}</span></div><div className="course-body"><h3>{c.title}</h3><p className="course-description">{c.description}</p><div className="course-meta"><span>{c.enrolledCount} students</span><span>{c.durationWeeks} weeks</span></div><div className="course-footer"><strong className="fee">{money(c.fee)}</strong><span className="pill">{c.status}</span></div></div></article>)}</div>}
          <div className="panel-card form-panel" style={{marginTop:24}}>
            <h3>Academic records and course communication</h3>
            <label>Course<select value={academicCourseId} onChange={e=>setAcademicCourseId(e.target.value)} required><option value="">Choose assigned course</option>{courses.filter(c=>user.role==='ADMIN'||c.teacherId===user.id).map(c=><option key={c.id} value={c.id}>{c.courseCode} — {c.title}</option>)}</select></label>
            {user.role === 'TEACHER' && academicCourseId && <div className="table-wrap" style={{marginTop:16,marginBottom:16}}><table><thead><tr><th>Student ID</th><th>Student name</th><th>Email</th><th>Enrolled on</th></tr></thead><tbody>{courseStudents.map(s=><tr key={s.id}><td>{s.id}</td><td>{s.fullName}</td><td>{s.email}</td><td>{new Date(s.enrolledAt).toLocaleDateString()}</td></tr>)}</tbody></table>{courseStudents.length===0&&<p className="tiny-note">No active students enrolled yet.</p>}</div>}
            <div className="form-two">
              <form className="stack-form" onSubmit={e=>{e.preventDefault();if(!academicCourseId){setError('Choose a course first');return}saveAcademic(academicCourseId,'attendance',{studentId:Number(academicForm.studentId),date:academicForm.date,present:academicForm.present,note:academicForm.note})}}>
                <h4>Record attendance</h4><label>Student ID<input type="number" min="1" required value={academicForm.studentId} onChange={e=>setAcademicForm({...academicForm,studentId:e.target.value})}/></label><label>Date<input type="date" required value={academicForm.date} onChange={e=>setAcademicForm({...academicForm,date:e.target.value})}/></label><label>Status<select value={academicForm.present?'true':'false'} onChange={e=>setAcademicForm({...academicForm,present:e.target.value==='true'})}><option value="true">Present</option><option value="false">Absent</option></select></label><label>Note<input value={academicForm.note} onChange={e=>setAcademicForm({...academicForm,note:e.target.value})}/></label><button className="primary">Save attendance</button>
              </form>
              <form className="stack-form" onSubmit={e=>{e.preventDefault();if(!academicCourseId){setError('Choose a course first');return}saveAcademic(academicCourseId,'grades',{studentId:Number(academicForm.studentId),assessmentName:academicForm.assessmentName,score:Number(academicForm.score),maxScore:Number(academicForm.maxScore),remarks:academicForm.remarks})}}>
                <h4>Record grade</h4><label>Student ID<input type="number" min="1" required value={academicForm.studentId} onChange={e=>setAcademicForm({...academicForm,studentId:e.target.value})}/></label><label>Assessment<input required value={academicForm.assessmentName} onChange={e=>setAcademicForm({...academicForm,assessmentName:e.target.value})}/></label><div className="form-two"><label>Score<input type="number" min="0" required value={academicForm.score} onChange={e=>setAcademicForm({...academicForm,score:e.target.value})}/></label><label>Maximum<input type="number" min="0.01" required value={academicForm.maxScore} onChange={e=>setAcademicForm({...academicForm,maxScore:e.target.value})}/></label></div><label>Remarks<input value={academicForm.remarks} onChange={e=>setAcademicForm({...academicForm,remarks:e.target.value})}/></label><button className="primary">Save grade</button>
              </form>
              <form className="stack-form" onSubmit={e=>{e.preventDefault();if(!academicCourseId){setError('Choose a course first');return}saveAcademic(academicCourseId,'assignments',{title:academicForm.title,description:academicForm.description,dueDate:academicForm.dueDate||null})}}>
                <h4>Create assignment</h4><label>Title<input required value={academicForm.title} onChange={e=>setAcademicForm({...academicForm,title:e.target.value})}/></label><label>Description<textarea value={academicForm.description} onChange={e=>setAcademicForm({...academicForm,description:e.target.value})}/></label><label>Due date<input type="date" value={academicForm.dueDate} onChange={e=>setAcademicForm({...academicForm,dueDate:e.target.value})}/></label><button className="primary">Publish assignment</button>
              </form>
              <form className="stack-form" onSubmit={e=>{e.preventDefault();if(!academicCourseId){setError('Choose a course first');return}saveAcademic(academicCourseId,'notices',{title:academicForm.title,message:academicForm.message})}}>
                <h4>Publish notice</h4><label>Title<input required value={academicForm.title} onChange={e=>setAcademicForm({...academicForm,title:e.target.value})}/></label><label>Message<textarea required value={academicForm.message} onChange={e=>setAcademicForm({...academicForm,message:e.target.value})}/></label><button className="primary">Publish notice</button>
              </form>
            </div>
            <p className="tiny-note">Use the Student ID shown in the student list for the selected course. Only actively enrolled students can receive academic records.</p>
          </div>
        </section>}
      </div>
    </main>
  </div>
}

function Stat({ label, value, detail, icon, onClick }: { label: string; value: string | number; detail: string; icon: string; onClick: () => void }) {
  return <button type="button" className="stat-card stat-card-action" onClick={onClick} aria-label={`View details for ${label}`}><div className="stat-top"><span>{label}</span><span className="stat-icon">{icon}</span></div><strong className="stat-value">{value}</strong><span className="stat-detail">{detail}</span></button>
}
function StatusRow({ label, value, total }: { label: string; value: number; total: number }) {
  return <div className="status-row"><div><span>{label}</span><strong>{value}</strong></div><div className="progress-track"><span style={{ width: `${Math.min(100, (value / total) * 100)}%` }} /></div></div>
}
function Empty({ title, text, action }: { title: string; text: string; action?: ReactNode }) {
  return <div className="empty-state"><div className="empty-icon">◫</div><h3>{title}</h3><p>{text}</p>{action}</div>
}