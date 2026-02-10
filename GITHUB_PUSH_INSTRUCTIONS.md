# GitHub Push Instructions

## Current Status

✅ **All code has been committed locally**
- Template syntax fixes (calendar.html, select-seats.html)
- Comprehensive test files (BookingServiceTest, UserServiceTest, AuthControllerTest)
- .gitignore file added
- All source files are committed

## Push to GitHub

The push failed due to an SSL certificate verification issue. Here are the solutions:

### Option 1: Fix SSL Certificate Issue (Recommended)

```bash
cd /Users/krishna.k/Documents/Project-One

# Try pushing again
git push origin commit
```

If you still get SSL errors, configure git to use system certificates:

```bash
# macOS - Set certificate path
git config --global http.sslCAInfo /etc/ssl/cert.pem

# Or disable SSL verification (less secure, but works)
git config --global http.sslVerify false

# Then push
git push origin commit
```

### Option 2: Use SSH Instead of HTTPS

1. **Change remote URL to SSH:**
   ```bash
   git remote set-url origin git@github.com:Airokun123/Project-One.git
   ```

2. **Push:**
   ```bash
   git push origin commit
   ```

### Option 3: Manual Push via GitHub Web Interface

1. Go to: https://github.com/Airokun123/Project-One
2. Create a new branch or go to the "commit" branch
3. Upload files manually if needed

## Verify What's Committed

Check what will be pushed:
```bash
git log origin/commit..HEAD --oneline
```

## Files Included in Latest Commit

- ✅ `.gitignore` - Excludes build artifacts and IDE files
- ✅ `src/test/java/com/office/booking/service/BookingServiceTest.java`
- ✅ `src/test/java/com/office/booking/service/UserServiceTest.java`
- ✅ `src/test/java/com/office/booking/controller/AuthControllerTest.java`
- ✅ `src/main/resources/templates/select-seats.html` (fixed syntax)
- ✅ `src/main/resources/templates/calendar.html` (fixed syntax)
- ✅ `TEST_INSTRUCTIONS.md`
- ✅ All other project files

## After Successful Push

Once pushed, you can:
1. View the code on GitHub
2. Clone it on another machine
3. Run tests: `mvn clean test`
4. Run the app: `mvn spring-boot:run`

## Troubleshooting

### If push still fails:

1. **Check your GitHub credentials:**
   ```bash
   git config --global user.name "Your Name"
   git config --global user.email "your.email@example.com"
   ```

2. **Use GitHub CLI (if installed):**
   ```bash
   gh auth login
   git push origin commit
   ```

3. **Check network connectivity:**
   ```bash
   ping github.com
   ```

## Current Branch

You're on branch: `commit`
Remote: `origin/commit`
Status: 2 commits ahead of origin
