// 🔴 HTML 문서가 완전히 불러와진 후에 스크립트가 실행되도록 보장한다. 🔴
document.addEventListener('DOMContentLoaded', () => {
    // 🔴 숨겨진 textarea에서 AI가 생성한 원시 데이터를 안전하게 가져온다. 🔴
    const rawDataElement = document.getElementById('raw-data-container');
    if (!rawDataElement) return;
    
    const rawData = rawDataElement.value;
    const sections = rawData.split('---SEP---');
    const paper = document.getElementById('examPaper');

    sections.forEach((section) => {
        if(section.trim().length < 20) return;

        const qMatch = section.match(/\[\[QUESTION\]\]([\s\S]*?)\[\[PASSAGE\]\]/);
        const pMatch = section.match(/\[\[PASSAGE\]\]([\s\S]*?)\[\[OPTIONS\]\]/);
        const oMatch = section.match(/\[\[OPTIONS\]\]([\s\S]*?)\[\[ANSWER\]\]/);
        const aMatch = section.match(/\[\[ANSWER\]\]([\s\S]*?)\[\[EXPLANATION\]\]/);
        const eMatch = section.match(/\[\[EXPLANATION\]\]([\s\S]*)/);

        if(qMatch && pMatch && oMatch) {
            const item = document.createElement('div');
            item.className = 'question-item';
            
            // 🔴 컨트롤러에서 받은 괄호 숫자(1)를 실제 모의고사용 동그라미 숫자(①)로 화면에서만 치환한다. 🔴
            let cleanOptions = oMatch[1].trim()
                .replace(/\(1\)/g, '①')
                .replace(/\(2\)/g, '②')
                .replace(/\(3\)/g, '③')
                .replace(/\(4\)/g, '④')
                .replace(/\(5\)/g, '⑤')
                .replace(/\n/g, '<br>');

            // 🔴 JS 파일에서는 외부 변수를 템플릿 리터럴로 바인딩할 때 역슬래시(\) 없이 일반 ${} 기호를 사용해야 한다. 🔴
            let content = `
                <div class="q-title">${qMatch[1].trim()}</div>
                <div class="q-passage">${pMatch[1].trim()}</div>
                <div class="q-options">${cleanOptions}</div>
                <div class="admin-only">
                    <p style="color:#d9534f; margin:0 0 5px 0;"><strong>✅ 정답:</strong> ${aMatch ? aMatch[1].trim() : ''}</p>
                    <p style="margin:0;"><strong>💡 해설:</strong> ${eMatch ? eMatch[1].trim() : ''}</p>
                </div>
            `;
            
            item.innerHTML = content;
            paper.appendChild(item);
        }
    });
});

// 🔴 학생용/교사용 화면 전환 로직을 수행한다. 🔴
function toggleMode(mode) {
    const admins = document.querySelectorAll('.admin-only');
    if(mode === 'teacher') {
        admins.forEach(el => el.classList.add('show'));
        document.getElementById('btn-tch').style.backgroundColor = '#ddd';
        document.getElementById('btn-std').style.backgroundColor = '#fff';
    } else {
        admins.forEach(el => el.classList.remove('show'));
        document.getElementById('btn-tch').style.backgroundColor = '#fff';
        document.getElementById('btn-std').style.backgroundColor = '#ddd';
    }
}

// 🔴 PDF 다운로드 스크립트 (A4 비율에 맞춰 고화질로 렌더링한다). 🔴
function downloadPDF() {
    const element = document.getElementById('pdf-area');
    const opt = {
        margin:       0,
        filename:     'AI_영어_모의고사.pdf',
        image:        { type: 'jpeg', quality: 1.0 },
        html2canvas:  { scale: 2, useCORS: true },
        jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' }
    };
    html2pdf().set(opt).from(element).save();
}