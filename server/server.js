const express = require('express');
const mysql = require('mysql2/promise');
const bcrypt = require('bcrypt');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const cors = require('cors');
const app = express();
const PORT = process.env.PORT || 8080;

// 디렉토리 생성
const uploadsDir = path.resolve(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir);
}
// 디렉토리 생성
const profile_uploadsDir = path.resolve(__dirname, 'profile_uploads');
if (!fs.existsSync(profile_uploadsDir)) {
    fs.mkdirSync(profile_uploadsDir);
}

// 미들웨어 설정
app.use(express.json());
app.use(cors());

// 이미지 저장을 위한 multer 설정
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadsDir);
    },
    filename: (req, file, cb) => {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});
const upload = multer({
    storage: storage, fileFilter: (req, file, cb) => {
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
        if (allowedTypes.includes(file.mimetype)) {
            cb(null, true);
        } else {
            cb(new Error('Invalid file type'));
        }
    }
});

// 프로필이미지 파일을 저장할 폴더와 파일명 설정
const profile_storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, profile_uploadsDir); // 프로필 이미지를 저장할 폴더
    },
    filename: (req, file, cb) => {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

const profile_upload = multer({ storage: profile_storage });

// MySQL 데이터베이스 연결 설정
const db = mysql.createPool({
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'password',
    database: 'to_yourism'
});

// 비동기 함수로 쿼리 처리
const dbQuery = async (sql, params) => {
    const [rows] = await db.execute(sql, params);
    return rows;
};

// 기본 경로 핸들러
app.get('/', (req, res) => {
    res.send('Welcome to the API!');
});

// 회원가입 엔드포인트
app.post('/register', async (req, res) => {
    try {
        const { email, password, nickname, phone_number } = req.body;
        if (!email || !password || !nickname || !phone_number) {
            return res.status(400).json({ success: false, message: '모든 필드를 입력해야 합니다.' });
        }

        const existingUserQuery = "SELECT * FROM users WHERE email = ?";
        const existingUser = await dbQuery(existingUserQuery, [email]);
        if (existingUser.length > 0) {
            return res.status(400).json({ success: false, message: '이미 존재하는 이메일입니다.' });
        }

        const hashedPassword = await bcrypt.hash(password, 10);
        const insertUserQuery = "INSERT INTO users (email, password, nickname, phone_number) VALUES (?, ?, ?, ?)";
        await dbQuery(insertUserQuery, [email, hashedPassword, nickname, phone_number]);

        res.status(201).json({ success: true, message: '회원가입 성공' });
    } catch (err) {
        console.error('회원가입 처리 중 오류 발생:', err);
        res.status(500).json({ success: false, message: '내부 서버 오류' });
    }
});

// 구글 로그인 엔드포인트
app.post('/google-login', async (req, res) => {
    try {
        const { google_email } = req.body;
        if (!google_email) {
            return res.status(400).json({ success: false, message: 'Google email is required' });
        }

        const sql = "SELECT * FROM users WHERE email = ?";
        const result = await dbQuery(sql, [google_email]);

        if (result.length === 0) {
            const sqlInsert = "INSERT INTO users (email, password, nickname, phone_number) VALUES (?, NULL, 'NewUser', '01000000000')";
            await dbQuery(sqlInsert, [google_email]);
            res.status(201).json({ success: true, message: 'Google user registered and logged in' });
        } else {
            res.status(200).json({ success: true, message: 'Google user logged in' });
        }
    } catch (err) {
        console.error('Error during Google login:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

// 로그인 엔드포인트
app.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ success: false, message: 'Email and password are required' });
        }

        const sql = "SELECT * FROM users WHERE email = ?";
        const result = await dbQuery(sql, [email]);

        if (result.length === 0) {
            return res.status(400).json({ success: false, message: 'Invalid email or password' });
        }

        const user = result[0];
        const isMatch = await bcrypt.compare(password, user.password);

        if (!isMatch) {
            return res.status(400).json({ success: false, message: 'Invalid email or password' });
        }

        res.status(200).json({ success: true, message: 'Login successful' });
    } catch (err) {
        console.error('Error during login:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

// 사용자 정보 조회 엔드포인트
app.post('/get-user-data', async (req, res) => {
    try {
        const { email } = req.body;
        if (!email) {
            return res.status(400).json({ success: false, message: 'Email is required' });
        }

        const sql = "SELECT email, password, nickname, phone_number, profile_image FROM users WHERE email = ?";
        const result = await dbQuery(sql, [email]);

        if (result.length === 0) {
            return res.status(404).json({ success: false, message: 'User not found' });
        }

        const user = result[0];
        res.status(200).json({ success: true, user });
    } catch (err) {
        console.error('Error fetching user data:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

// 사용자 정보 업데이트 엔드포인트
app.post('/update-user-data', async (req, res) => {
    try {
        const { email, newNickname, newPhoneNumber, newPassword, profileImage } = req.body;
        if (!email || !newNickname || !newPhoneNumber) {
            return res.status(400).json({ success: false, message: 'Email, nickname, and phone number are required' });
        }

        let updateFields = [newNickname, newPhoneNumber];
        let updateQuery = "UPDATE users SET nickname = ?, phone_number = ?";

        if (newPassword) {
            const hashedPassword = await bcrypt.hash(newPassword, 10);
            updateFields.push(hashedPassword);
            updateQuery += ", password = ?";
        }

        if (profileImage) {
            updateFields.push(profileImage);
            updateQuery += ", profile_image = ?";
        }

        updateQuery += " WHERE email = ?";
        updateFields.push(email);

        await dbQuery(updateQuery, updateFields);

        res.status(200).json({ success: true, message: 'User data updated successfully' });
    } catch (err) {
        console.error('Error updating user data:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

// 게시물 조회 엔드포인트
app.get('/posts', async (req, res) => {
    const validSortOptions = ['latest', 'likes'];
    const sort = validSortOptions.includes(req.query.sort) ? req.query.sort : 'latest';
    const location = req.query.location || 'all';

    let sql = "SELECT * FROM posts";
    let params = [];

    if (location !== 'all') {
        sql += " WHERE region LIKE ?";
        params.push(`${location}%`); // `location`으로 시작하는 모든 지역을 검색
    }

    if (sort === 'latest') {
        sql += " ORDER BY created_at DESC";
    } else if (sort === 'likes') {
        sql += " ORDER BY likes DESC";
    }

    sql += " LIMIT 10";

    console.log(`SQL Query: ${sql}`);
    console.log(`Parameters: ${params}`);
    console.log(`Requested Sort: ${sort}`);
    console.log(`Requested Location: ${location}`);

    try {
        const posts = await dbQuery(sql, params);
        res.status(200).json(posts);
    } catch (err) {
        console.error('Error fetching posts:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

// 이미지 업로드 엔드포인트
app.post('/upload-image', upload.single('image'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ success: false, message: 'No file uploaded' });
    }

    const imageUrl = `/uploads/${req.file.filename}`;
    res.status(200).json({ success: true, imageUrl });
});

// 이미지 파일을 제공하는 엔드포인트
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 프로필 이미지 업로드 엔드포인트
app.post('/upload-profile-image', profile_upload.single('profile-image'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ success: false, message: 'No file uploaded' });
        }

        const imageUrl = `/profile_uploads/${req.file.filename}`;
        const email = req.body.email; // 클라이언트에서 이메일을 받아야 합니다.

        if (!email) {
            return res.status(400).json({ success: false, message: 'Email is required' });
        }

        // 사용자 데이터베이스에 프로필 이미지 URL 저장
        const sql = "UPDATE users SET profile_image = ? WHERE email = ?";
        const result = await dbQuery(sql, [imageUrl, email]);

        if (result.affectedRows === 0) {
            return res.status(404).json({ success: false, message: 'User not found' });
        }

        res.status(200).json({ success: true, imageUrl });
    } catch (err) {
        console.error('Error updating profile image:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

// 정적 파일 제공 설정
app.use('/profile_uploads', express.static(path.join(__dirname, 'profile_uploads'), {
    setHeaders: (res, path) => {
        if (path.endsWith('.jpg') || path.endsWith('.jpeg')) {
            res.setHeader('Content-Type', 'image/jpeg');
        } else if (path.endsWith('.png')) {
            res.setHeader('Content-Type', 'image/png');
        } else if (path.endsWith('.gif')) {
            res.setHeader('Content-Type', 'image/gif');
        }
    }
}));

// 게시물 생성 엔드포인트
app.post('/create-post', async (req, res) => {
    try {
        const { title, content, region, images, email } = req.body;

        // 요청 데이터 유효성 검사
        if (!title || !content || !images || !email) {
            return res.status(400).json({ success: false, message: '제목, 내용, 이미지, 이메일이 필요합니다.' });
        }

        console.log('요청 데이터:', { title, content, region, images, email }); // 요청 데이터 확인

        // 이메일로 닉네임 조회
        const nicknameResult = await dbQuery("SELECT nickname FROM users WHERE email = ?", [email]);

        console.log('이메일:', email); // 이메일 확인
        console.log('닉네임 조회 결과:', nicknameResult); // 닉네임 조회 결과 확인

        if (nicknameResult.length === 0) {
            return res.status(404).json({ success: false, message: '사용자를 찾을 수 없습니다.' });
        }

        const author = nicknameResult[0].nickname;

        console.log('작성자 닉네임:', author); // 닉네임 확인

        // 게시물 생성 쿼리문
        const insertPostQuery = `
            INSERT INTO posts (title, content, region, images, likes, created_at, author)
            VALUES (?, ?, ?, ?, ?, NOW(), ?)
        `;

        const result = await dbQuery(insertPostQuery, [
            title,
            content,
            region,
            JSON.stringify(images), // JSON 문자열로 변환하여 저장
            0, // 초기 좋아요 수
            author // 작성자의 닉네임
        ]);

        res.status(201).json({ success: true, message: '게시물이 성공적으로 생성되었습니다.', postId: result.insertId });
    } catch (err) {
        // 오류 로그 출력
        console.error('게시물 생성 중 오류 발생:', err.stack);
        res.status(500).json({ success: false, message: '내부 서버 오류', error: err.message });
    }
});



// 게시글 상세 정보 조회 API
app.get('/posts/:id', async (req, res) => {
    const postId = req.params.id;

    try {
        const sql = "SELECT * FROM posts WHERE id = ?";
        const post = await dbQuery(sql, [postId]);

        if (post.length > 0) {
            res.json(post[0]);
        } else {
            res.status(404).send('Post not found');
        }
    } catch (err) {
        console.error('Error fetching post details:', err);
        res.status(500).send('Server error');
    }
});
// 게시글 좋아요 엔드포인트
app.post('/posts/:id/like', async (req, res) => {
    const postId = req.params.id;

    try {
        // 좋아요 수를 1 증가시키는 쿼리
        const updateLikesQuery = "UPDATE posts SET likes = likes + 1 WHERE id = ?";
        const result = await dbQuery(updateLikesQuery, [postId]);

        if (result.affectedRows === 0) {
            return res.status(404).json({ success: false, message: 'Post not found' });
        }

        res.status(200).json({ success: true, message: 'Like added successfully' });
    } catch (err) {
        console.error('Error adding like to post:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

// 서버 시작
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});